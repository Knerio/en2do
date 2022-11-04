package eu.koboo.en2do.test.customer.tests;

import eu.koboo.en2do.MongoManager;
import eu.koboo.en2do.test.Const;
import eu.koboo.en2do.test.customer.Customer;
import eu.koboo.en2do.test.customer.CustomerRepository;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CustomerFindByIdTest {

    static MongoManager manager;
    static CustomerRepository repository;

    @BeforeAll
    public static void setup() {
        System.out.println(CustomerFindByIdTest.class.getName() + " START");
        manager = new MongoManager();
        assertNotNull(manager);
        repository = manager.create(CustomerRepository.class);
        assertNotNull(repository);
    }

    @Test
    @Order(1)
    public void cleanUpRepository() {
        List<Customer> customerList = repository.findAll();
        assertNotNull(customerList);
        assertTrue(customerList.isEmpty());
    }

    @Test
    @Order(2)
    public void saveCustomer() {
        Customer customer = Const.createNewCustomer();
        assertNotNull(customer);
        assertFalse(repository.exists(customer));
        assertTrue(repository.save(customer));
        assertTrue(repository.exists(customer));
    }

    @Test
    @Order(3)
    public void findCustomer() {
        assertTrue(repository.existsById(Const.UNIQUE_ID));
        Customer customer = repository.findById(Const.UNIQUE_ID);
        assertNotNull(customer);
        assertEquals(Const.FIRST_NAME, customer.getFirstName());
        assertEquals(Const.LAST_NAME, customer.getLastName());
        assertEquals(Const.BIRTHDAY, customer.getBirthday());
        assertEquals(Const.PHONE_NUMBER, customer.getPhoneNumber());
        assertEquals(Const.ORDERS.size(), customer.getOrders().size());
    }

    @AfterAll
    public static void finish() {
        System.out.println(CustomerFindByIdTest.class.getName() + " END");
        assertTrue(repository.drop());
        assertTrue(manager.close());
    }
}
