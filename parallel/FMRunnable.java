import java.util.ArrayList;
import java.util.BitSet;

import javax.print.attribute.standard.OutputDeviceAssigned;

class FMRunnable implements Runnable {
    public int index;
    // stuff for storing the starting data
    public FM_vertex root;
    public static int[][] method; // the ringing method used to search

    public ArrayList<Character> calls;
    public ArrayList<String> path;
    public BitSet rung;

    public ArrayList<String> lead;

    public ArrayList<ArrayList<Character>> compositions;
    public int numcompositions;

    public StartData initial;

    public FMRunnable(int index, int[][] method, StartData sd) {
        this.index = index;
        FMRunnable.method = method;
        this.calls = sd.calls;
        this.path = sd.path;
        this.rung = sd.rung;
        this.compositions = new ArrayList<ArrayList<Character>>();
        this.numcompositions = 0;
        this.initial = sd;
        // constructor for the starting data
    }

    /*
       *************
       all the classes that help implement run()
       *************
    */

    public void generate_lead(FM_vertex v) {
        // includes next lead head

        ArrayList<String> currentlead = new ArrayList<String>();
        
        for (int i=0; i < method[0].length; i++) {
            char[] row = new char[method.length];
            for (int j = 0; j < method.length; j++) {
                row[method[j][i]] = v.lead_head.charAt(j);
            }
            currentlead.add(new String(row));
        }
        

        String next_lh_plain = currentlead.get(currentlead.size()-1).toString();

        String next_lh_bob = "" + next_lh_plain.charAt(0) + next_lh_plain.charAt(3) + next_lh_plain.substring(1, 3) + next_lh_plain.substring(4);

        String next_lh_single = currentlead.get(currentlead.size()-2).substring(0, 4) + next_lh_plain.substring(4);
        // defined as a "1234"

        char call = 'X';
        char call2 = 'x';

        switch (next_lh_bob.indexOf("8")) {
            case 5:
                call = 'M';
                break;
            case 6:
                call = 'W';
                break;
            case 7:
                call = 'H';
                break;
            case 2:
                call = 'B';
                break;
            default:
                call = 'X';

        }

        switch (next_lh_single.indexOf("8")) {
            case 5:
                call2 = 'm';
                break;
            case 6:
                call2 = 'w';
                break;
            case 7:
                call2 = 'h';
                break;
            case 2:
                call2 = 'b';
                break;
            default:
                call2 = 'x';

        }

        v.plain = new FM_vertex(next_lh_plain, 'p');
        v.bob = new FM_vertex(next_lh_bob, call);
        v.single = new FM_vertex(next_lh_single, call2);


        // temporarily exclude Before calls here
        // if (call == 'B') {
        //     v.bob.setVisited(true);
        // }

        // if (!tenorsTogether(next_lh_bob)) {
        //     v.bob.setVisited(true);
        // }

        // System.out.println(next_lh_plain + " " + next_lh_bob);
        currentlead.remove(currentlead.size()-1);
        lead = currentlead;

    }

    public boolean updateBitSet() {
        // temporary array for ints being added in this lead
        ArrayList<Integer> added = new ArrayList<Integer>();

        for (String row : lead) {
            
            int rowInt = rowToInt(row);

            if (rung.get(rowInt)) {
                // one row of this lead was already rung, so return false and set all
                // the previous rows from this lead back to unrung

                for (int i : added) {
                    rung.set(i, false);
                }
                
                return false;
            }
            rung.set(rowInt);
            added.add(rowInt);
        
        }
        return true;
    }

    public static Boolean tenorsTogether (String lh) {
        // 17864523 -> 8765324
        //             21357642
        String course_order = "" + lh.charAt(2) + lh.charAt(1) + lh.charAt(3) + lh.charAt(5) + lh.charAt(7) + lh.charAt(6) + lh.charAt(4) + lh.charAt(2);
        if (course_order.contains("87")) {
            return true;
        }
        return false;
    }

    public void updateForPred() {
        if (!lead.get(0).equals("12345678")) {
            for (String row : lead) {
                rung.set(rowToInt(row), false);
                //rungstr.remove(row);
            }
        }
        // calls.remove(calls.size()-1);
        // path.remove(path.size()-1);
    }

    public void removeFromBitSet(String lead_head) {
        ArrayList<String> lead = lead(lead_head);
        for (String row : lead) {
            rung.set(rowToInt(row), false);
        }
    }

    private void outputComposition(ArrayList<Character> calls) {

        int numrows = ((calls.size())*32);

        // length is between 5000 and 5600 inclusive, so return path
        //if (numrows >= 1250) {
        //if (numrows >= 5000) {
            //returns++;
            
            // formatting path and calls output
            ArrayList<String> output = new ArrayList<String>();
            String stroutput = new String();
            //this.calls.add(lastcall);

            String len_cat = "";

            if ((numrows >= 1250) && (numrows <= 1599)) {
                len_cat = "quarter peal";
            } else if ((numrows >= 2500) && (numrows <= 2999)) {
                len_cat = "half peal";
            } else if ((numrows >= 5000) && (numrows <= 5600)) {
                len_cat = "peal";
            } else if ((numrows >= 5377) && (numrows <= 9999)) {
                len_cat = "long peal";
            } else if (numrows >= 10000) {
                len_cat = "long length";
            }
            
            System.out.println(len_cat + " composition is of length " + (calls.size()+1) + " leads = " + numrows + " rows");
            
            if (len_cat == "quarter peal") {
                ArrayList<Character> changes = condensePlains(calls);
                outputCalls(changes);
            }
            System.out.println("----\n");
             
            //path.remove(path.size()-1);
            //calls.remove(path.size()-1);
            //}    
    }  

    public static int rowToInt(String rowstr) {
        int[] factorial = {1, 1, 2, 6, 24, 120, 720, 5040}; // lookup table for n!, 0 <= n <= 7
        int numbells = rowstr.length();                     // number of bells in the row
        
        // turn string into array of int
        char[] rowchars = rowstr.toCharArray();
        int[] row = new int[numbells];

        // the "value" of this row as an int, [0, (n!)-1]
        int value = 0;

        // subtract 1 from every digit
        for (int i = 0; i < numbells; i++) {
            row[i] = Character.getNumericValue(rowchars[i]) - 1;
        }

        for (int i = 0; i < numbells; i++) {                // for each digit
            value += row[i] * factorial[numbells - 1 - i];  // multiply by (len-1-index)! and add to total
            for (int j = i+1; j < numbells; j++) { 
                if (row[j] > row[i]) {                      // if any digits to the right are greater
                    row[j]--;                               // subtract 1 from them
                }
            }
        }
        return value;
    }
    
    public static ArrayList<String> lead(String lead_head) {
        // doesn't include next lead head

        ArrayList<String> currentlead = new ArrayList<String>();
        for (int i=0; i < method[0].length-1; i++) {
            char[] row = new char[method.length];
            for (int j = 0; j < method.length; j++) {
                row[method[j][i]] = lead_head.charAt(j);
            }
            currentlead.add(new String(row));
        }

        return(currentlead);

    }
    
    public ArrayList<Character> condensePlains(ArrayList<Character> calls) {

        int plains = 0;
        ArrayList<Character> finalcalls = new ArrayList<Character>();

        for (char c : calls) {
            if (c == 'p') {
                plains++;
            } else {
                if (plains > 0) {
                    char plainschar = (char)('0' + plains);

                    // plainschar = '-';
                    // finalcalls.add(plainschar);
                }
                plains = 0;
                finalcalls.add(c);
            }
        }

        return(finalcalls);


    }
    
    private void outputCalls(ArrayList<Character> calls) {
        System.out.println("in outputCalls with " + calls);
        ArrayList<Character> calling = (ArrayList<Character>) calls.clone();
        ArrayList<String> rows = new ArrayList<String>();
        boolean hasBefore = true;
        if (calling.contains('B')) {
            hasBefore = true;
        }
        int middles = 0; int wrongs = 0; int homes = 0; int befores = 0;
        int smiddles = 0; int swrongs = 0; int shomes = 0; int sbefores = 0;
        if (calling.size() == 0) {
            System.out.println("plain course");
            return;
        } 
        while (calling.size() > 0) {
            while ((calling.size() > 0) && (calling.get(0).equals('B'))) {
                befores++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('b'))) {
                sbefores++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('M'))) {
                middles++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('m'))) {
                smiddles++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('W'))) {
                wrongs++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('w'))) {
                swrongs++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('H'))) {
                homes++;
                calling.remove(0);
            }
            while ((calling.size() > 0) && (calling.get(0).equals('h'))) {
                shomes++;
                calling.remove(0);
            }
            
            String row = "";

            if (sbefores == 1) {
                befores = 9;
            }
            if (smiddles == 1) {
                middles = 9;
            }
            if (swrongs == 1) {
                wrongs = 9;
            }
            if (shomes == 1) {
                homes = 9;
            }

            if (hasBefore) {
                row = ("" + String.valueOf(befores) + String.valueOf(middles) + String.valueOf(wrongs) + String.valueOf(homes)).replace('1', '-').replace('0', ' ').replace('9', 's');
            } else {
                row = ("" + String.valueOf(middles) + String.valueOf(wrongs) + String.valueOf(homes)).replace('1', '-').replace('0', ' ');
            }
            middles = 0; wrongs = 0; homes = 0; befores = 0;
            smiddles = 0; swrongs = 0; shomes = 0; sbefores = 0;
            rows.add(row);
        }
        
        if (hasBefore) {
            System.out.println("B  M  W  H");
            for (String row : rows) {
                System.out.println(row.charAt(0) + "  " + row.charAt(1) + "  " + row.charAt(2) + "  " + row.charAt(3));
            }
        } else {
            System.out.println("M  W  H");
            for (String row : rows) {
                System.out.println(row.charAt(0) + "  " + row.charAt(1) + "  " + row.charAt(2));
            }
        }    
        System.out.println("\n");    
        return;
    }
    
    /* 
        the dfs one 
    */

    private void dynamic_visit(FM_vertex v, FM_vertex p) {

        // System.out.println("starting dynamic_visit from" + v.lead_head + v.getSuccessors());
        
        // boolean to indicate whether next step will be to go back up one level to the predecessor
        boolean backToPred = false;

        v.setVisited(true); // update as now visited
        v.setPredecessor(p); // set predecessor vertex
        
        generate_lead(v); // creates lead and successors for vertex
        FM_vertex[] successors = v.getSuccessors();

        // updates the current path and call path at this node
        path.add(v.lead_head);
        calls.add(v.call);
        
        // check if tenors together, if not then set successors as visited to not continue search there
        if (!tenorsTogether(v.lead_head)) {
            for (FM_vertex s : successors) {
                s.setVisited(true);
            }
            backToPred = true;
        }

        // checks for repetition using bitset
        if(!v.lead_head.equals("12345678") && !updateBitSet()) {
            //duplicates++;
            
            for (FM_vertex s : successors) {
                s.setVisited(true);
            }
        }

        // indicates next step is to go up one level to predecessor
        if (backToPred) {
            updateForPred();
            // returns the path, calls etc to their state before this node visited
        }


        for (FM_vertex s : successors) { // go through all successor vertices

            String lastInPath = path.get(path.size()-1);

            // backtracking up the tree the correct number of levels
            while (!(lastInPath.equals(v.lead_head))) {
                removeFromBitSet(lastInPath);
                path.remove(path.size()-1);
                calls.remove(calls.size()-1);
                lastInPath = path.get(path.size()-1);
            }

            if (s.lead_head.equals("12345678")) {
                // indicates that we have a true composition
                s.setVisited(true);
                numcompositions++;
                // make a copy of the current array of calls and add to list of compositions

                
                int numrows = ((calls.size()+2)*32);
                System.out.println(numrows + "\t" + numcompositions);
                // String len_cat = "";
                // if ((numrows >= 1250) && (numrows <= 1599)) {
                //     len_cat = "quarter peal";
                // } else if ((numrows >= 2500) && (numrows <= 2999)) {
                //     len_cat = "half peal";
                // } else if ((numrows >= 5000) && (numrows <= 5600)) {
                //     len_cat = "peal";
                // } else if ((numrows >= 5377) && (numrows <= 9999)) {
                //     len_cat = "long peal";
                // } else if (numrows >= 10000) {
                //     len_cat = "long length";
                // }

                // length is between 5000 and 5600 inclusive, so add composition
                if ((numrows >= 5000) && (numrows <= 5600)) {
                    //System.out.println(path + "\n" + calls + "\t" + initial.calls);
                    //compositions.add(composition);
                    numcompositions++;
                    System.out.println("composition found in " + index + ": " + numcompositions);

                    ArrayList<Character> composition = (ArrayList<Character>) calls.clone();
                    composition.add(s.call);
                    outputComposition(composition);
                    //outputCalls(condensePlains(composition));
                }
            }

            
            // prevents visiting a lead where the lead head has already been rung (false composition)
            if ((rung.get(rowToInt(s.lead_head))) && (!s.lead_head.equals("12345678"))) {
                s.setVisited(true);
            }

            for (String next_row : lead(s.lead_head)) {
                if (rung.get(rowToInt(next_row))) {
                    s.setVisited(true);
                }
            }

            // prevents visiting leads where the tenors are not together
            if (!tenorsTogether(s.lead_head)) {
                s.setVisited(true);
            }
            

            if (!s.getVisited()) {   // if vertex has not been visited
                dynamic_visit(s, v); // continue dfs search from it
            }
        }
        return;
    }


    public void run() {

        // the root of the sub-tree we are searching, take from end of passed in path
        FM_vertex rt = new FM_vertex(path.get(path.size()-1), '-');

        generate_lead(rt);
        FM_vertex[] L = rt.getSuccessors();
    
        for (FM_vertex v : L) {
            if (!v.getVisited()) {
                dynamic_visit(v, rt);
            }
        }

        for (ArrayList<Character> composition : compositions) {
            outputComposition(composition);
        }
    }
}

