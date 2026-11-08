package model;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserDetails {

    private Long userId;
    private String FirstName;
    private String MiddleName;
    private String LastName;
    private LocalDate Birthdate;
    private LocalDateTime CreatedAt;

//  ==============================
//  |  CONSTRUCTOR
//  ==============================

    public UserDetails(){

    }

    public String getFullName() {
        return FirstName +  " " + MiddleName + " " + LastName;
    }

}
