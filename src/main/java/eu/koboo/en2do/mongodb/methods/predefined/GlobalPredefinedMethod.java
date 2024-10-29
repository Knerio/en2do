package eu.koboo.en2do.mongodb.methods.predefined;

import com.mongodb.client.model.Filters;
import eu.koboo.en2do.mongodb.RepositoryData;
import eu.koboo.en2do.repository.Repository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bson.conversions.Bson;

import java.lang.reflect.Method;

/**
 * This class is a representation of a predefined method from the repository
 */
@Getter
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
@RequiredArgsConstructor
public abstract class GlobalPredefinedMethod {

    String methodName;

    /**
     * Invokes the method and returns the created object.
     *
     * @param <E>            The generic type of the entity
     * @param <ID>           The generic type of the entity id
     * @param <R>            The generic type of the repository
     * @param repositoryData The repository meta of the called repository
     * @param method         The method, which should be invoked
     * @param arguments      The object array, which represents the arguments of the method
     * @return The object created by the method invocation
     * @throws Exception any, if something bad happens
     */
    public abstract <E, ID, R extends Repository<E, ID>> Object handle(RepositoryData<E, ID, R> repositoryData,
                                                                       Method method,
                                                                       Object[] arguments) throws Exception;

    protected <ID> Bson createIdFilter(ID uniqueId) {
        return Filters.eq("_id", uniqueId);
    }

    protected Bson createIdExistsFilter() {
        return Filters.exists("_id");
    }

}
