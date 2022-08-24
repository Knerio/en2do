package eu.koboo.en2do.test.cases;

import eu.koboo.en2do.MongoManager;
import eu.koboo.en2do.Repository;
import eu.koboo.en2do.Scope;
import eu.koboo.en2do.test.Const;
import eu.koboo.en2do.test.customer.Customer;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class SyncOperationTest {

    static MongoManager manager;
    static Repository<Customer, UUID> repository;
    static Scope<Customer, UUID> scope;

    @BeforeClass
    public static void before() {
        System.out.println(SyncOperationTest.class.getName() + " starting.");
        manager = new MongoManager();
        assertNotNull(manager);
        repository = manager.createRepository();
        assertNotNull(repository);
        scope = repository.createScope();
        assertNotNull(scope);
    }

    @Test
    public void operationTest() {
        assertNotNull(Const.CUSTOMER);
        assertTrue(repository.save(Const.CUSTOMER));

        Bson bsonFilter = scope.eq(Customer::getUniqueId, Const.UNIQUE_ID);
        assertTrue(repository.exists(bsonFilter));

        Customer customer = repository.find(bsonFilter);
        assertNotNull(customer);

        assertEquals(Const.UNIQUE_ID, customer.getUniqueId());
        assertEquals(Const.FIRST_NAME, customer.getFirstName());
        assertEquals(Const.LAST_NAME, customer.getLastName());
        assertEquals(Const.BIRTHDAY, customer.getBirthday());
    }
    
    @AfterClass
    public static void after() {
        System.out.println(SyncOperationTest.class.getName() + " ending.");
        assertTrue(repository.deleteAll());
        assertTrue(manager.close());
    }
}