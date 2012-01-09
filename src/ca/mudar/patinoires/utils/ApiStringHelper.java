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

import ca.mudar.patinoires.io.RemoteRinksHandler.RemoteValues;
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
        if (open.equals(RemoteValues.BOOLEAN_FALSE)) {
            return DbValues.CONDITION_CLOSED;
        }
        else if (condition.equals(RemoteValues.RINK_CONDITION_EXCELLENT)) {
            return DbValues.CONDITION_EXCELLENT;
        }
        else if (condition.equals(RemoteValues.RINK_CONDITION_GOOD)) {
            return DbValues.CONDITION_GOOD;
        }
        else if (condition.equals(RemoteValues.RINK_CONDITION_BAD)) {
            return DbValues.CONDITION_BAD;
        }
        else {
            /**
             * Default to Unknown condition.
             */
            return DbValues.CONDITION_UNKNOWN;
        }
    }

    public static int getTypeIndex(String type) {
        if (type.equals(RemoteValues.RINK_TYPE_PSE)) {
            return DbValues.KIND_PSE;
        }
        else if (type.equals(RemoteValues.RINK_TYPE_PP)) {
            return DbValues.KIND_PP;
        }
        else if (type.equals(RemoteValues.RINK_TYPE_C)) {
            return DbValues.KIND_C;
        }
        else {
            /**
             * Default to Free skating.
             */
            return DbValues.KIND_PPL;
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
        else if (descFr.equals("Patinoire de patin libre no 2")) {
            return "Free skating rink #2";
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
        else if (descFr.equals("Grande patinoire avec bandes")
                || descFr.equals("Patinoire avec bandes grande")) {
            return "Big rink with boards";
        }
        else if (descFr.equals("Petite patinoire avec bandes")
                || descFr.equals("Patinoire avec bandes petite")) {
            return "Small rink with boards";
        }
        else if (descFr.equals("Patinoire entretenue par les citoyens")) {
            return "Rink maintained by citizens";
        }
        else if (descFr.equals("Pat. avec bandes - près chalet")) {
            return "Rink with boards - Near Chalet";
        }
        else if (descFr.equals("Pat. avec bandes - près 10e Avenue")) {
            return "Rink with boards - Near 10th Avenue";
        }
        else {
            return descFr;
        }
    }

}
