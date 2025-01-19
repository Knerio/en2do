package eu.koboo.en2do.test.user;

import eu.koboo.en2do.test.RepositoryTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class UserRepositoryTest extends RepositoryTest<User, UUID, UserRepository> {

    @Override
    public Class<UserRepository> repositoryClass() {
        return UserRepository.class;
    }

//    @Test
//    public void drop() {
//        repository.drop();
//    }

    @Test
    public void test() throws InterruptedException {
        repository.deleteAll();
        UUID userId = UUID.randomUUID();
        User user = User.builder().userName("TestName").uniqueId(userId).email("test@test.com").build();

        repository.save(user);

        Thread.sleep(2000);

        Assertions.assertNotNull(repository.findAll().getFirst().getSubField().id);
    }
}
