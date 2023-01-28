package index;

public class Tag {
    String serialNumber; // also used as the name of local image
    String name;
    boolean isNew;
    Dan dan;
    StarTitle starTitle;
    String starNumber; // "<number>", "スペシャル", or "?". "?" means it is not "スペシャル", but the exact number of stars
                       // is unknown
}
