package datatypes;

import java.io.Serializable;

public class Building implements Serializable {
    private String url = null; // used to identify this building to the API
    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Building)) return false;

        Building building = (Building) o;

        if (!url.equals(building.url)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Building{" +
                "name='" + name + '\'' +
                '}';
    }

    public Building(String name) {
        this.name = name;
    }

    public Building(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}

