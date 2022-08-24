package eu.koboo.en2do.test.cases.conversion;

import eu.koboo.en2do.MongoManager;
import eu.koboo.en2do.test.Assertion;
import eu.koboo.en2do.test.Const;
import eu.koboo.en2do.test.customer.Customer;
import eu.koboo.en2do.test.customer.CustomerRepository;
import org.bson.Document;
import org.junit.AfterClass;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConversionTest {

    static MongoManager manager;
    static CustomerRepository repository;

    @BeforeClass
    public static void before() {
        System.out.println(ConversionTest.class.getName() + " starting.");
        manager = new MongoManager();
        assertNotNull(manager);
        repository = new CustomerRepository(manager);
        assertNotNull(repository);
    }

    @Test
    public void conversionTest() throws Exception {
        assertNotNull(Const.CUSTOMER);
        Document originalDocument = repository.toDocument(Const.CUSTOMER);
        assertNotNull(originalDocument);
        assertEquals("Rainer", originalDocument.getString("firstName"));
        assertEquals("Zufall", originalDocument.getString("lastName"));
        assertEquals("12.12.2012", originalDocument.getString("birthday"));
        Customer fromDocument = repository.toEntity(originalDocument);
        assertNotNull(fromDocument);
        assertEquals("Rainer", fromDocument.getFirstName());
        assertEquals("Zufall", fromDocument.getLastName());
        assertEquals("12.12.2012", fromDocument.getBirthday());
        Assertion.assertContains(fromDocument.getOrderNumbers(), 1);
        Assertion.assertContains(fromDocument.getOrderNumbers(), 2);
        Assertion.assertContains(fromDocument.getOrderNumbers(), 3);
        Assertion.assertContains(fromDocument.getOrderNumbers(), 4);
        Assertion.assertContains(fromDocument.getOrderNumbers(), 5);
    }

    @AfterClass
    public static void after() {
        System.out.println(ConversionTest.class.getName() + " ending.");
        assertTrue(manager.close());
    }
}