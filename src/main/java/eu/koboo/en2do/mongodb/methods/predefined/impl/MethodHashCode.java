package eu.koboo.en2do.mongodb.methods.predefined.impl;

import eu.koboo.en2do.mongodb.RepositoryData;
import eu.koboo.en2do.mongodb.methods.predefined.GlobalPredefinedMethod;
import eu.koboo.en2do.repository.Repository;

import java.lang.reflect.Method;

public class MethodHashCode extends GlobalPredefinedMethod {

    public MethodHashCode() {
        super("hashCode");
    }

    @Override
    public <E, ID, R extends Repository<E, ID>> Object handle(RepositoryData<E, ID, R> repositoryData,
                                                              Method method, Object[] arguments) {
        Class<R> repositoryClass = repositoryData.getRepositoryClass();
        return repositoryClass.getName().hashCode() + repositoryData.getCollectionName().hashCode();
    }
}
