package eu.koboo.en2do.test.cases;

import eu.koboo.en2do.MongoManager;
import eu.koboo.en2do.test.Const;
import eu.koboo.en2do.test.customer.Customer;
import eu.koboo.en2do.test.customer.CustomerRepository;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FindByBalanceBetweenTest {

    static MongoManager manager;
    static CustomerRepository repository;

    @BeforeAll
    public static void setup() {
        System.out.println(FindByBalanceBetweenTest.class.getName() + " starting.");
        manager = new MongoManager();
        assertNotNull(manager);
        repository = manager.create(CustomerRepository.class);
        assertNotNull(repository);
    }

    @Test
    @Order(1)
    public void cleanUpRepository() {
        System.out.println("CleanUp Repository");
        assertTrue(repository.deleteAll());
        List<Customer> customerList = repository.findAll();
        assertNotNull(customerList);
        assertTrue(customerList.isEmpty());
    }

    @Test
    @Order(2)
    public void saveCustomer() {
        System.out.println("Save Customer");
        Customer customer = Const.createNew();
        assertNotNull(customer);
        assertTrue(repository.save(customer));
        assertTrue(repository.exists(customer));
    }

    @Test
    @Order(3)
    public void operationTest() {
        System.out.println("Test Operation");
        List<Customer> customerList = repository.findByBalanceBetweenAndCustomerId(100, 600, 1);
        assertNotNull(customerList);
        assertFalse(customerList.isEmpty());
        assertEquals(1, customerList.size());
        for (Customer customer : customerList) {
            assertEquals(Const.FIRST_NAME, customer.getFirstName());
            assertEquals(Const.LAST_NAME, customer.getLastName());
            assertEquals(Const.BIRTHDAY, customer.getBirthday());
            assertEquals(Const.PHONE_NUMBER, customer.getPhoneNumber());
            assertEquals(Const.ORDERS.size(), customer.getOrders().size());
        }
    }
    
    @AfterAll
    public static void finish() {
        System.out.println(FindByBalanceBetweenTest.class.getName() + " ending.");
        assertTrue(repository.deleteAll());
        assertTrue(manager.close());
    }
}