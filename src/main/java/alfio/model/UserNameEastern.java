package alfio.model;

public class UserNameEastern extends UserName{
    public UserNameEastern(String firstName, String lastName) {
        super(firstName, lastName);
    }

    public String getFullName() {
        return (firstName != null && lastName != null) ? (lastName + " " + firstName) : fullName;
    }
}
