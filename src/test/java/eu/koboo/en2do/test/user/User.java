package eu.koboo.en2do.test.user;

import eu.koboo.en2do.repository.entity.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

@Getter // lombok
@Setter // lombok
@NoArgsConstructor // lombok
@FieldDefaults(level = AccessLevel.PRIVATE) // lombok
@ToString // lombok
@Builder
@AllArgsConstructor
public class User {

    @Id // en2do
    UUID uniqueId;

    String userName;
    String email;

    Date registrationDate;
    Date lastLoginDate;
    Date inactiveDate;

    @Builder.Default
    SubField subField = new SubField.SubFieldBuilder().build();

    public String someThing() {
        return "Awesome";
    }

    public String getCombined() {
        return userName + email;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubField {

        @Id
        @Builder.Default
        UUID id = UUID.randomUUID();

        @Builder.Default
        int age = new Random().nextInt(20);

    }

}
