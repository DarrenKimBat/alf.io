package alfio.model;

public class UserNameWestern extends UserName{
    public UserNameWestern(String firstName, String lastName) {
        super(firstName, lastName);
    }

    public String getFullName() {
        return (firstName != null && lastName != null) ? (firstName + " " + lastName) : fullName;
    }

}
