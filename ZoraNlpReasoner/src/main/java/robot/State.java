/**
 * This class models the status of the robot
 */

package main.java.robot;

import main.java.data.Pair;
import main.java.managers.OntologyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class State {

    /***
     * Robot operating modes
     *
     */
    public enum Mode {
        SEQUENTIAL,
        HOLD
    }

    private String head;
    private String eyesAnimation;
    private String eyesColor;
    private String rightArm;
    private String leftArm;
    private String rightHand;
    private String leftHand;
    private String rightLeg;
    private String leftLeg;
    private String robot;

    private Mode operatingMode;


    /***
     * Initial robots state builder.
     * It is initialized to the StandInit position.
     * Operating mode is read from the ontology.
     *
     */
    public State() {

        this.robot = "stand";
        this.head = "forward";
        this.eyesAnimation = "static";
        this.eyesColor = "white";
        this.rightArm = "down";
        this.leftArm = "down";
        this.rightHand = "close";
        this.leftHand = "close";
        this.rightLeg = "down";
        this.leftLeg = "down";

        String mode = OntologyManager.OperatingModeQuery();

        switch(mode){
            case "SEQUENTIAL":
                this.operatingMode = Mode.SEQUENTIAL;
                break;
            case "HOLD":
                this.operatingMode = Mode.HOLD;
                break;
            default:
                break;
        }

        this.printState();
    }

    /**
     * Checks that the new action is compatible with the status of the robot and with the previous actions.
     * @param newActionCode new action code
     * @param actions list of actions to be performed
     * @return "compatible" if the new action is compatible;
     * else part of the robot that makes the action incompatible
     */
    public String verifyActionCompatibility(String newActionCode, List<String> actions) {

        // If the robot is in SEQUENTIAL mode and if a new sentence is starting
        if(this.operatingMode == Mode.SEQUENTIAL && actions.isEmpty()) {

            actions.addAll(this.resetRobotState());

            return "compatible";
        }

        // Read the actions incompatible with the new one
        List<Pair<String, String>> incompatibleActions = OntologyManager.compatibilityQuery(newActionCode);

        // For all incompatible actions, checks that the robot has not performed one
        for(Pair<String, String> incompatibleAction : incompatibleActions){

            switch(String.valueOf(incompatibleAction.getRight())){
                case "head":
                    if(this.head.equalsIgnoreCase(incompatibleAction.getLeft())) {
                        return incompatibleAction.getRight() + " " + incompatibleAction.getLeft();
                    }
                    break;
                case "arm":
                    if((this.rightArm.equalsIgnoreCase(incompatibleAction.getLeft()) )
                        || (this.leftArm.equalsIgnoreCase(incompatibleAction.getLeft())) ) {
                        return incompatibleAction.getRight() + " " + incompatibleAction.getLeft();
                    }
                    break;
                case "hand":
                    if((this.rightHand.equalsIgnoreCase(incompatibleAction.getLeft()) )
                            || (this.leftHand.equalsIgnoreCase(incompatibleAction.getLeft())) ) {
                        return incompatibleAction.getRight() + " " + incompatibleAction.getLeft();
                    }
                    break;
                case "leg":
                    if((this.rightLeg.equalsIgnoreCase(incompatibleAction.getLeft()) )
                            || (this.leftLeg.equalsIgnoreCase(incompatibleAction.getLeft())) ) {
                        return incompatibleAction.getRight() + " " + incompatibleAction.getLeft();
                    }
                    break;
                case "null":
                    if(this.robot.equalsIgnoreCase(incompatibleAction.getLeft())) {
                        return incompatibleAction.getLeft();
                    }
                    break;
                default:
                    break;
            }
        }

        System.out.println("--- New Action " + newActionCode + " COMPATIBLE ---");

        return "compatible";
    }

    /**
     * Updates robot status after performs the action
     * @param vb verb keyword of action
     * @param part robot body part involved in action, "" if use whole robot
     * @param side body side "left" o "right", "" if not used
     * @param color eyes color, "" if not used
     */
    public void updateState(String vb, String part, String side, String color) {

        switch(side){
            case "left":
                switch(part){
                    case "arm":
                        this.leftArm = vb;
                        break;
                    case "hand":
                        switch(vb){
                            case "open":
                            case "close":
                                this.leftHand = vb;
                                break;
                            default:
                                this.leftArm = vb;
                                break;
                        }
                        break;
                    case "leg":
                        this.leftLeg = vb;
                        break;
                }
                break;

            case "right":
                switch(part){
                    case "arm":
                        this.rightArm = vb;
                        break;
                    case "hand":
                        switch(vb){
                            case "open":
                            case "close":
                                this.rightHand = vb;
                                break;
                            default:
                                this.rightArm = vb;
                                break;
                        }
                        break;
                    case "leg":
                        this.rightLeg = vb;
                        break;
                }
                break;

            default:
                switch(part){
                    case "eyes":
                        switch(vb){
                            case "change color":
                                this.eyesColor = color;
                                break;
                            default:
                                this.eyesAnimation = vb;
                                break;
                        }
                        break;
                    case "head":
                        this.head = vb;
                        break;
                    case "arms":
                        this.leftArm = vb;
                        this.rightArm = vb;
                        break;
                    case "hands":
                        switch(vb){
                            case "open":
                            case "close":
                                this.leftHand = vb;
                                this.rightHand = vb;
                                break;
                            default:
                                this.leftArm = vb;
                                this.rightArm = vb;
                                break;
                        }
                        break;
                    default:
                        this.robot = vb;
                }
        }

    }

    /**
     * Changes robot operating mode, only if is different from its current mode.
     * @param newMode new mode
     * @return action to warn of changes,
     * for RESET also the actions to restore the state of the robot,
     * null if the mode don't change
     */
    public List<String> changeRobotMode(Mode newMode) {

        if(this.operatingMode == newMode)
            return new ArrayList<>(Arrays.asList(""));

        this.operatingMode = newMode;

        switch(this.operatingMode){
            case HOLD:
                return new ArrayList<>(Arrays.asList("SAY-MH"));
            case SEQUENTIAL:
                operatingMode = Mode.SEQUENTIAL;

                List<String> actions = new ArrayList<>();
                actions.add("SAY-MS");

                // Resetta lo stato del robot
                actions.addAll(this.resetRobotState());
                return actions;
        }

        return new ArrayList<>(Arrays.asList(""));
    }

    /**
     * Restores robot to initial position
     *
     * @return action codes to add in the action list
     */
    public List<String> resetRobotState(){

        List<String> actions = new ArrayList<>();

        // If the robot has a raised leg, lowers it
        if(this.leftLeg.equalsIgnoreCase("up"))
            actions.add("LL-1");

        if(this.rightLeg.equalsIgnoreCase("up"))
            actions.add("LR-1");

        // Returns in Stand
        actions.add("RR-00");

        // If the animation of the eyes has changed, they become static again
        if(!this.eyesAnimation.equalsIgnoreCase("static"))
            actions.add("ELER-0");

        // If the color of the eyes has changed, they become white again
        if(!this.eyesColor.equalsIgnoreCase("white"))
            actions.add("ELER-C:255,255,255");

        this.robot = "stand";
        this.head = "forward";
        this.eyesAnimation = "static";
        this.eyesColor = "white";
        this.rightArm = "down";
        this.leftArm = "down";
        this.rightHand = "close";
        this.leftHand = "close";
        this.rightLeg = "down";
        this.leftLeg = "down";

        return actions;
    }

    /**
     * Prints current robot status
     *
     */
    public void printState() {

        System.out.println("\nZORA STATUS");
        System.out.println("Mode:\t" + this.operatingMode);
        System.out.print("Robot:\t" + this.robot);
        if(this.robot == "stand")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Head:\t" + this.head);
        if(this.head == "forward")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Eyes Anim:\t" + this.eyesAnimation);
        if(this.eyesAnimation == "static")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Eyes Color:\t" + this.eyesColor);
        if(this.eyesColor == "white")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Right Arm:\t" + this.rightArm);
        if(this.rightArm == "down")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Left Arm:\t" + this.leftArm);
        if(this.leftArm == "down")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Right Hand:\t" + this.rightHand);
        if(this.rightHand == "close")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Left Hand:\t" + this.leftHand);
        if(this.leftHand == "close")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Right Leg:\t" + this.rightLeg);
        if(this.rightLeg == "down")
            System.out.print(" [default]");
        System.out.println();
        System.out.print("Left Leg:\t" + this.leftLeg);
        if(this.leftLeg == "down")
            System.out.print(" [default]");
        System.out.println();

    }

}
