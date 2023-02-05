package eu.koboo.en2do.internal.exception.repository;

public class RepositoryEntityNotFoundException extends Exception {

    public RepositoryEntityNotFoundException(Class<?> repoClass, Throwable cause) {
        super("The Entity class of " + repoClass.getName() + " could not be found!", cause);
    }
}