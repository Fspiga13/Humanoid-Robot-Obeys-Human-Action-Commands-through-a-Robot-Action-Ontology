/**
 * This class models a Word dependency
 */

package main.java.data;

public class Dependency {

    private String tagDependency;
    private String reference;

    /**
     * Word dependency builder.
     * @param tagDependency dependency tag name
     * @param reference word linked to the dependency
     */
    public Dependency(String tagDependency, String reference){

        this.tagDependency = tagDependency;
        this.reference = reference;
    }

    /**
     * @return dependency tag
     *
     */
    public String getTagDependency() {
        return tagDependency;
    }

    /**
     * @return word linked to the dependency
     *
     */
    public String getReference() {
        return reference;
    }

}
