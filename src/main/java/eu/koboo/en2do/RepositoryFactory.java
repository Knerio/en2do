package eu.koboo.en2do;

import com.mongodb.client.MongoCollection;
import eu.koboo.en2do.repository.annotation.Id;
import eu.koboo.en2do.repository.exception.*;
import eu.koboo.en2do.sort.annotation.Limit;
import eu.koboo.en2do.sort.annotation.Skip;
import eu.koboo.en2do.sort.annotation.SortFields;
import eu.koboo.en2do.sort.annotation.SortBy;
import eu.koboo.en2do.repository.FilterOperator;
import eu.koboo.en2do.repository.FilterType;
import eu.koboo.en2do.repository.MethodOperator;
import eu.koboo.en2do.sort.Sort;
import eu.koboo.en2do.utility.GenericUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RepositoryFactory {

    MongoManager manager;
    Map<Class<?>, Repository<?, ?>> repoRegistry;

    public RepositoryFactory(MongoManager manager) {
        this.manager = manager;
        this.repoRegistry = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    protected <E, ID, R extends Repository<E, ID>> R create(Class<R> repoClass) throws Exception {
        if (repoRegistry.containsKey(repoClass)) {
            return (R) repoRegistry.get(repoClass);
        }

        // Parse Entity and UniqueId classes
        Type[] repoGenericTypeArray = repoClass.getGenericInterfaces();
        Type repoGenericTypeParams = null;
        for (Type type : repoGenericTypeArray) {
            if (type.getTypeName().split("<")[0].equalsIgnoreCase(Repository.class.getName())) {
                repoGenericTypeParams = type;
                break;
            }
        }
        if (repoGenericTypeParams == null) {
            throw new RepositoryNoTypeException(repoClass);
        }
        String entityClassName = repoGenericTypeParams.getTypeName().split("<")[1].split(",")[0];
        Class<E> entityClass;
        Class<ID> entityUniqueIdClass = null;
        Set<String> entityFieldNameSet = new HashSet<>();
        Field entityUniqueIdField = null;
        try {
            entityClass = (Class<E>) Class.forName(entityClassName);
            Field[] declaredFields = entityClass.getDeclaredFields();
            if (declaredFields.length == 0) {
                throw new RepositoryNoFieldsException(repoClass);
            }
            for (Field field : declaredFields) {
                String lowerFieldName = field.getName().toLowerCase(Locale.ROOT);
                if (entityFieldNameSet.contains(lowerFieldName)) {
                    throw new RepositoryDuplicatedFieldException(field, repoClass);
                }
                entityFieldNameSet.add(lowerFieldName);
                if (Modifier.isFinal(field.getModifiers())) {
                    throw new RepositoryFinalFieldException(field, repoClass);
                }
                if (!field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                entityUniqueIdClass = (Class<ID>) field.getType();
                entityUniqueIdField = field;
                entityUniqueIdField.setAccessible(true);
            }
            if (entityUniqueIdClass == null) {
                throw new RepositoryIdNotFoundException(entityClass);
            }
        } catch (ClassNotFoundException e) {
            throw new RepositoryEntityNotFoundException(repoClass, e);
        }

        // These methods are ignored by our methods processing.
        List<String> ignoredMethods = Arrays.asList(
                // Predefined methods by Repo interface
                "getCollectionName", "getUniqueId", "getEntityClass", "getEntityUniqueIdClass",
                "findById", "findAll", "delete", "deleteById", "deleteAll", "drop",
                "save", "saveAll", "exists", "existsById",
                // Predefined methods by Java
                "toString", "hashCode", "equals", "notify", "notifyAll", "wait", "finalize", "clone");
        for (Method method : repoClass.getMethods()) {
            if (ignoredMethods.contains(method.getName())) {
                continue;
            }
            // Check for the return-types of the methods, and their defined names to match our pattern.
            checkReturnTypes(method, entityClass, repoClass);
            checkMethodOperation(method, entityClass, repoClass);
            checkSortOptions(method, entityClass, repoClass);
        }

        // Parse predefined collection name and create mongo collection
        if (!repoClass.isAnnotationPresent(eu.koboo.en2do.repository.annotation.Repository.class)) {
            throw new RepositoryNameNotFoundException(repoClass);
        }
        String entityCollectionName = repoClass.getAnnotation(eu.koboo.en2do.repository.annotation.Repository.class).value();
        MongoCollection<E> entityCollection = manager.getDatabase().getCollection(entityCollectionName, entityClass);

        // Create dynamic repository proxy object
        ClassLoader repoClassLoader = repoClass.getClassLoader();
        Class<?>[] interfaces = new Class[]{repoClass};
        Class<Repository<E, ID>> actualClass = (Class<Repository<E, ID>>) repoClass;
        Repository<E, ID> repository = (Repository<E, ID>) Proxy.newProxyInstance(repoClassLoader, interfaces,
                new RepositoryInvocationHandler<>(this, entityCollectionName, entityCollection,
                        actualClass, entityClass, entityUniqueIdClass,
                        entityUniqueIdField));
        repoRegistry.put(repoClass, repository);
        return (R) repository;
    }

    private <E> void checkReturnTypes(Method method, Class<E> entityClass, Class<?> repoClass) throws Exception {
        Class<?> returnTypeClass = method.getReturnType();
        String methodName = method.getName();
        MethodOperator methodOperator = MethodOperator.parseMethodStartsWith(methodName);
        if (methodOperator == null) {
            throw new MethodNoMethodOperatorException(method, repoClass);
        }
        switch (methodOperator) {
            case FIND -> {
                if (GenericUtils.isTypeOf(List.class, returnTypeClass)) {
                    Class<?> listType = GenericUtils.getGenericTypeOfReturnList(method);
                    if (!listType.isAssignableFrom(entityClass)) {
                        throw new MethodFindListTypeException(method, entityClass, listType);
                    }
                    return;
                }
                if (GenericUtils.isTypeOf(entityClass, returnTypeClass)) {
                    return;
                }
                throw new MethodFindReturnTypeException(method, entityClass, repoClass);
            }
            case DELETE, EXISTS -> {
                if (!GenericUtils.isTypeOf(Boolean.class, returnTypeClass)) {
                    throw new MethodBooleanReturnTypeException(method, repoClass);
                }
            }
            case COUNT -> {
                if (!GenericUtils.isTypeOf(Long.class, returnTypeClass)) {
                    throw new MethodLongReturnTypeException(method, repoClass);
                }
            }
            default -> throw new MethodUnsupportedReturnTypeException(returnTypeClass, methodName, entityClass);
        }
    }

    // ReturnType already checked.
    private <E> void checkMethodOperation(Method method, Class<E> entityClass, Class<?> repoClass) throws Exception {
        String methodName = method.getName();
        MethodOperator methodOperator = MethodOperator.parseMethodStartsWith(methodName);
        if (methodOperator == null) {
            throw new MethodNoMethodOperatorException(method, repoClass);
        }
        String fieldFilterName = methodOperator.removeOperatorFrom(methodName);
        if (methodName.contains("And") && methodName.contains("Or")) {
            throw new MethodDuplicatedChainException(method, entityClass);
        }
        int expectedParameters = countExpectedParameters(entityClass, repoClass, method, fieldFilterName);
        Debugger.print("Method: " + method.getName());
        Debugger.print("ExpParams: " + expectedParameters);
        int parameterCount = method.getParameterCount();
        Debugger.print("ActParams: " + parameterCount);
        if (expectedParameters != parameterCount) {
            if (parameterCount > 0) {
                Class<?> lastParam = method.getParameterTypes()[parameterCount - 1];
                Debugger.print("lastParam: " + lastParam.getName());
                Debugger.print("AddParam: " + (expectedParameters + 1));
                Debugger.print("Assignable: " + lastParam.isAssignableFrom(Sort.class));
                if (lastParam.isAssignableFrom(Sort.class) && (expectedParameters + 1) != parameterCount) {
                    throw new MethodParameterCountException(method, repoClass, (expectedParameters + 1), parameterCount);
                }
            } else {
                throw new MethodParameterCountException(method, repoClass, expectedParameters, parameterCount);
            }
        }
        String[] fieldFilterSplitByType = fieldFilterName.contains("And") ?
                fieldFilterName.split("And") : fieldFilterName.split("Or");
        int nextIndex = 0;
        for (int i = 0; i < fieldFilterSplitByType.length; i++) {
            String fieldFilterPart = fieldFilterSplitByType[i];
            FilterType filterType = createFilterType(entityClass, repoClass, method, fieldFilterPart);
            checkParameterType(method, filterType, nextIndex, repoClass);
            nextIndex = i + filterType.operator().getExpectedParameterCount();
        }
    }

    private <E> int countExpectedParameters(Class<E> entityClass, Class<?> repoClass, Method method, String fieldFilterPart) throws Exception {
        String methodName = method.getName();
        if (!methodName.contains("And") && !methodName.contains("Or")) {
            FilterType filterType = createFilterType(entityClass, repoClass, method, fieldFilterPart);
            return filterType.operator().getExpectedParameterCount();
        }
        String[] fieldFilterPartSplitByType = fieldFilterPart.contains("And") ?
                fieldFilterPart.split("And") : fieldFilterPart.split("Or");
        int expectedCount = 0;
        for (String fieldFilterPartSplit : fieldFilterPartSplitByType) {
            FilterType filterType = createFilterType(entityClass, repoClass, method, fieldFilterPartSplit);
            expectedCount += filterType.operator().getExpectedParameterCount();
        }
        return expectedCount;
    }

    private <E> void checkParameterType(Method method, FilterType filterType, int startIndex, Class<?> repoClass) throws Exception {
        int expectedParamCount = filterType.operator().getExpectedParameterCount();
        Debugger.print("Check params for " + filterType.operator().name());
        for (int i = 0; i < expectedParamCount; i++) {
            int paramIndex = startIndex + i;
            Class<?> paramClass = method.getParameters()[paramIndex].getType();
            if (paramClass == null) {
                throw new MethodParameterNotFoundException(method, repoClass, (startIndex + expectedParamCount),
                        method.getParameterCount());
            }
            // Special check for regex, because it has only String or Pattern parameters
            if (filterType.operator() == FilterOperator.REGEX) {
                if (!GenericUtils.isTypeOf(String.class, paramClass) && !GenericUtils.isTypeOf(Pattern.class, paramClass)) {
                    throw new MethodInvalidRegexParameterException(method, repoClass, paramClass);
                }
                return;
            }
            Class<?> fieldClass = filterType.field().getType();
            if (filterType.operator() == FilterOperator.IN) {
                if (!GenericUtils.isTypeOf(List.class, paramClass)) {
                    throw new MethodMismatchingTypeException(method, repoClass, List.class, paramClass);
                }
                Class<?> listType = GenericUtils.getGenericTypeOfParameterList(method, paramIndex);
                if (!GenericUtils.isTypeOf(fieldClass, listType)) {
                    throw new MethodInvalidListParameterException(method, repoClass, fieldClass, listType);
                }
                return;
            }
            if (!GenericUtils.isTypeOf(fieldClass, paramClass)) {
                throw new MethodMismatchingTypeException(method, repoClass, fieldClass, paramClass);
            }
        }
    }

    protected <E> FilterType createFilterType(Class<E> entityClass, Class<?> repoClass, Method method, String operatorString) throws Exception {
        FilterOperator filterOperator = FilterOperator.parseFilterEndsWith(operatorString);
        if (filterOperator == null) {
            throw new MethodNoFilterOperatorException(method, repoClass);
        }
        String expectedField = filterOperator.removeOperatorFrom(operatorString);
        expectedField = expectedField.endsWith("Not") ? expectedField.replaceFirst("Not", "") : expectedField;
        Field field = findExpectedField(entityClass, expectedField);
        if (field == null) {
            throw new MethodFieldNotFoundException(expectedField, method, entityClass, repoClass);
        }
        return new FilterType(field, filterOperator);
    }

    private <E> Field findExpectedField(Class<E> entityClass, String expectedField) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (!field.getName().equalsIgnoreCase(expectedField)) {
                continue;
            }
            return field;
        }
        return null;
    }

    private <E> void checkSortOptions(Method method, Class<E> entityClass, Class<?> repoClass) throws Exception {
        String fieldName = null;
        if (method.isAnnotationPresent(SortBy.class)) {
            SortBy sortBy = method.getAnnotation(SortBy.class);
            fieldName = sortBy.field();
        }
        int parameterCount = method.getParameterCount();
        if (parameterCount > 0) {
            Class<?> lastParam = method.getParameterTypes()[parameterCount - 1];
            boolean hasAnySortAnnotation = method.isAnnotationPresent(Limit.class)
                    || method.isAnnotationPresent(Skip.class)
                    || method.isAnnotationPresent(SortBy.class)
                    || method.isAnnotationPresent(SortFields.class);
            if (hasAnySortAnnotation && lastParam.isAssignableFrom(Sort.class)) {
                throw new MethodMixedSortException(method, repoClass);
            }
        }
        if(fieldName == null) {
            return;
        }
        Field field = findExpectedField(entityClass, fieldName);
        if (field == null) {
            throw new MethodSortFieldNotFoundException(fieldName, method, entityClass, repoClass);
        }
    }
}