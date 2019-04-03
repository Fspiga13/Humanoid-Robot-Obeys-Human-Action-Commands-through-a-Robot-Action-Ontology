/**
 * This class models a Word with its POS tag and dependencies.
 */

package main.java.data;

import java.util.ArrayList;
import java.util.List;

public class Word {

    private String posTag;
    private List<Dependency> dependencies;

    /**
     * Word builder
     *
     */
    public Word(){
        dependencies = new ArrayList<>();
    }

    /**
     * @return word POS tag
     *
     */
    public String getPosTag() {
        return posTag;
    }

    /**
     * Sets Word POS tag
     * @param posTag word POS tag
     */
    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }

    /**
     * Adds a new dependency
     * @param tagDependency dependency tag
     * @param reference word linked to the dependency
     */
    public void addDependency(String tagDependency, String reference) {
        dependencies.add(new Dependency(tagDependency, reference));
    }

    /**
     * Search if the input tag is present between the
     * word dependency tags and returns the associated word.
     * @param tagDep dependency tag to search
     * @return word linked to the dependency, null if tag is not found
     */
    public String getDependency(String tagDep){

        for(Dependency dependency : dependencies){
            if(dependency.getTagDependency().equalsIgnoreCase(tagDep))
                return dependency.getReference();
        }
        return null;
    }

    /**
     * Search if the input tag is present between the
     * word dependency tags and returns the associated words.
     * @param tagDep dependency tag to search
     * @return list of words linked to the dependency, null if tag is not found
     */
    public List<String> getDependencies(String tagDep){

        List<String> deps = new ArrayList<>();

        for(Dependency dependency : dependencies){
            if(dependency.getTagDependency().equalsIgnoreCase(tagDep))
                deps.add(dependency.getReference());
        }
        return deps;
    }

}
