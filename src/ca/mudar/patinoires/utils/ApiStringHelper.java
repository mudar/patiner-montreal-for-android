/*
    Patiner Montréal for Android.
    Information about outdoor rinks in the city of Montréal: conditions,
    services, contact, map, etc.

    Copyright (C) 2010 Mudar Noufal <mn@mudar.ca>

    This file is part of Patiner Montréal for Android.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.mudar.patinoires.utils;

import ca.mudar.patinoires.utils.Const.DbValues;

public class ApiStringHelper {
    /**
     * This function helps define "Open/Closed" as a 4th condition besides
     * excellent/good/bad. Logically, conditions cannot be "good" when the rink
     * is closed!
     * 
     * @param condition Description (in words) of the condition
     * @return the index used in the DB for conditions
     */
    static public int getConditionIndex(String open, String condition) {
        condition = condition.toLowerCase();
        open = open.toLowerCase();
        if (open.equals("false")) {
            return DbValues.CONDITION_CLOSED;
        }
        else if (condition.equals("excellent") || condition.equals("excellente")) {
            return 0;
        }
        else if (condition.equals("good") || condition.equals("bonne")) {
            return 1;
        }
        else if (condition.equals("bad") || condition.equals("mauvaise")) {
            return 2;
        }
        else {
            // Log.e( TAG , "Rink is open (" + open + "). Condition not found ("
            // + condition + ")." );
            return DbValues.CONDITION_CLOSED;
        }
    }

    /**
     * Manual translation.. yeah!
     * 
     * @param descFr
     * @return Translation into English
     */
    static public String translateRinkDescription(String descFr) {

        if (descFr.equals("Patinoire de patin libre")) {
            return "Free skating rink";
        }
        else if (descFr.equals("Patinoire avec bandes")) {
            return "Rink with boards";
        }
        else if (descFr.equals("Patinoire décorative")) {
            return "Landskaped rink";
        }
        else if (descFr.equals("Aire de patinage libre")) {
            return "Free skating area";
        }
        else if (descFr.equals("Patinoire réfrigérée")) {
            return "Refrigerated rink";
        }
        else if (descFr.equals("Patinoire de patin libre no 1")) {
            return "Free skating rink #1";
        }
        else if (descFr.equals("Patinoire avec bandes no 1")) {
            return "Rink with boards #1";
        }
        else if (descFr.equals("Patinoire avec bandes no 2")) {
            return "Rink with boards #2";
        }
        else if (descFr.equals("Patinoire avec bandes no 3")) {
            return "Rink with boards #3";
        }
        else if (descFr.equals("Patinoire avec bandes nord")) {
            return "Rink with boards North";
        }
        else if (descFr.equals("Patinoire avec bandes sud")) {
            return "Rink with boards South";
        }
        else if (descFr.equals("Grande patinoire avec bandes")) {
            return "Big rink with boards";
        }
        else if (descFr.equals("Petite patinoire avec bandes")) {
            return "Small rink with boards";
        }
        else {
            return descFr;
        }
    }
}
