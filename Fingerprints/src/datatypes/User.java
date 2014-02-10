package datatypes;

public class User {
    private final String username;
    private final String password;
    private String user;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUrl(){
        return user;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
