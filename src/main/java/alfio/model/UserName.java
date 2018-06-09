package alfio.model;

public abstract class UserName {
    protected String fullName;
    protected String firstName;
    protected String lastName;

    public UserName(String firstName, String lastName){
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName(){
        return firstName;
    }

    public String getLastName(){
        return lastName;
    }

    public abstract String getFullName();
}
