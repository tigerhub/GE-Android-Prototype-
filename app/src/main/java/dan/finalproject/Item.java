package manda094.finalproject;


// this class is used for displaying the "anomaly id" + "anomaly deviation",
// and for displaying the "machine id" + "report status / last report time".
// it can also be used to other similar occasions in this project
public class Item {

    // Instance Variables
    // --------------------------------------------------------------------------
    private String title;			// these are just name place-holders; they can be changed to mean anything
    private String description;

    // Constructor
    // --------------------------------------------------------------------------
    public Item(String title, String description) {
        super();
        this.title = title;
        this.description = description;
    }

    // getters and setters...
    // --------------------------------------------------------------------------
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

