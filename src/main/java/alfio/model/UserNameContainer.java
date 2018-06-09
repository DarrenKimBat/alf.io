package alfio.model;

public final class UserNameContainer {
    enum WESTERN{
        UNITED_STATES, CANADA, ITALY, SWITZERLAND, UNITED_KINGDOM, AUSTRALIA, SOUTH_AFRICA;
    }
    enum EASTERN{
        CHINA, KOREA, JAPAN;
    }

    private static UserNameContainer userNameContainer;
    private UserNameContainer(){ }

    public static UserNameContainer Instance(){
        if(userNameContainer == null){
            userNameContainer = new UserNameContainer();
        }

        return userNameContainer;
    }

    public static UserName getNameNotation(String Country, String firstName, String lastName) {
        //init by western name notation
        UserName userName = new UserNameWestern(firstName, lastName);

        for (WESTERN western : WESTERN.values()) {
            if (western.name().equals(Country.toUpperCase())) {
                userName = new UserNameWestern(firstName, lastName);
            }
        }

        for (EASTERN eastern : EASTERN.values()) {
            if (eastern.name().equals(Country.toUpperCase())) {
                userName = new UserNameEastern(firstName, lastName);
            }
        }

        return userName;
    }
}

