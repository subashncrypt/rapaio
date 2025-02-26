/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.experiment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import rapaio.data.Frame;
import rapaio.data.SolidFrame;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.data.VarNominal;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
@Deprecated
public class JavaDBUtil {

    private Connection conn;

    public void connect() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        conn = DriverManager.getConnection("jdbc:derby:memory:m;create=true");
    }

    public void putFrame(Frame df, String tableName) throws SQLException {
        String[] columns = df.varNames();
        String[] types = new String[columns.length];
        for (int i = 0; i < types.length; i++) {
            if (df.rvar(i).type().isNumeric()) {
                types[i] = "DOUBLE";
                continue;
            }
            if (df.rvar(i).type().isNominal()) {
                types[i] = "VARCHAR(8000)";
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (");
        for (int i = 0; i < columns.length; i++) {
            sb.append(columns[i]).append(" ").append(types[i]);
            if (i != columns.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sb.toString());
        }

        sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < columns.length; i++) {
            sb.append(columns[i]);
            if (i != columns.length - 1) {
                sb.append(",");
            }
        }
        sb.append(") VALUES (");
        for (int i = 0; i < columns.length; i++) {
            sb.append("?");
            if (i != columns.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
            for (int i = 0; i < df.rowCount(); i++) {
                for (int j = 0; j < types.length; j++) {
                    switch (types[j]) {
                        case "VARCHAR(8000)" -> ps.setString(j + 1, df.getLabel(i, j));
                        case "DOUBLE" -> ps.setDouble(j + 1, df.getDouble(i, j));
                    }
                }
                ps.execute();
            }
        }
    }

    public Frame getFrame(String query) throws SQLException {
        try (Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData md = rs.getMetaData();
            List<String> colNames = new ArrayList<>();
            List<List<Object>> lists = new ArrayList<>();
            for (int i = 0; i < md.getColumnCount(); i++) {
                colNames.add(md.getColumnLabel(i + 1));
                lists.add(new ArrayList<>());
            }
            while (rs.next()) {
                for (int i = 0; i < md.getColumnCount(); i++) {
                    String sqlTypeName = md.getColumnTypeName(i + 1);
                    switch (sqlTypeName) {
                        case "DOUBLE", "INTEGER" -> lists.get(i).add(rs.getDouble(i + 1));
                        default -> lists.get(i).add(rs.getString(i + 1));
                    }
                }
            }
            List<Var> vars = new ArrayList<>();
            for (int i = 0; i < md.getColumnCount(); i++) {
                String sqlTypeName = md.getColumnTypeName(i + 1);
                switch (sqlTypeName) {
                    case "DOUBLE", "INTEGER" -> {
                        VarDouble v1 = VarDouble.empty(lists.get(i).size());
                        for (int j = 0; j < lists.get(i).size(); j++) {
                            v1.setDouble(j, (Double) lists.get(i).get(j));
                        }
                        vars.add(v1);
                    }
                    default -> {
                        ArrayList<String> dict = new ArrayList<>();
                        for (int j = 0; j < lists.get(i).size(); j++) {
                            dict.add((String) lists.get(i).get(j));
                        }
                        VarNominal v2 = VarNominal.empty(lists.get(i).size(), dict);
                        for (int j = 0; j < lists.get(i).size(); j++) {
                            v2.setLabel(j, (String) lists.get(i).get(j));
                        }
                        vars.add(v2);
                    }
                }
            }
            for (int i = 0; i < vars.size(); i++) {
                vars.get(i).name(colNames.get(i));
            }
            return SolidFrame.byVars(lists.get(0).size(), vars);
        }
    }
}
