package br.com.pereiraeng.sql.swing;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import br.com.pereiraeng.modelling.modelutils.uml.UMLfield;
import br.com.pereiraeng.modelling.swing.UMLobjectD;
import br.com.pereiraeng.sql.SQLadapter;

public class SQLUMLbuilder {

	/**
	 * Função que converte uma base de dados em uma coleção de objetos UML contendo
	 * os campos das tabelas
	 * 
	 * @param sql     conector SQL
	 * @param library biblioteca
	 * @return lista de objetos UML
	 */
	public static List<UMLobjectD> buildUMLobjectsFromTables(SQLadapter sql, String library) {
		DatabaseMetaData dbmd = sql.getMetaData();
		if (dbmd == null)
			return null;

		List<UMLobjectD> out = new LinkedList<>();
		try {
			ResultSet rs = dbmd.getColumns(null, library, null, "%");
			while (rs.next()) {
				String table = rs.getString(3).trim();
				String field = rs.getString(4).trim();
				int type = rs.getInt(5);

				UMLobjectD uo = null;
				for (UMLobjectD u : out) {
					if (table.equals(u.getNome())) {
						uo = u;
						break;
					}
				}
				if (uo == null) {
					out.add(uo = new UMLobjectD(table, null));
					uo.setDrawable(true);
					uo.setLoc((float) Math.random(), (float) Math.random());
				}

				uo.add(new UMLfield(type, field, null));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return out;
	}
}
