package br.com.pereiraeng.sql.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import br.com.pereiraeng.core.StringUtils;
import br.com.pereiraeng.core.collections.ArrayUtils;
import br.com.pereiraeng.sql.SQLadapter;
import br.com.pereiraeng.sql.DatabaseEngine;
import br.com.pereiraeng.swing.SwingUtils;
import br.com.pereiraeng.swing.button.CUDpanel;
import br.com.pereiraeng.swing.dialog.FillingFields;
import br.com.pereiraeng.swing.table.AdvancedTableModel;

/**
 * Classe do objeto gráfico com uma tabela que exibe o conteúdo das tabelas da
 * base de dados SQL
 * 
 * @author Philipe PEREIRA
 *
 */
public class SQLtableEditor extends JPanel implements ActionListener {
	private static final long serialVersionUID = 1L;

	private JLabel tableName;
	private JTable table;
	private AdvancedTableModel model;

	// ---------------------------------------------

	private SQLadapter sql;

	private int[] tableTypes;

	/**
	 * vetor com as posições das chaves primárias da tabela
	 */
	private int[] keys;

	/**
	 * Construtor do objeto gráfico que exibe as entradas da tabela da base de dados
	 * SQL
	 * 
	 * @param sql objeto que estabelece a conexão com a base de dados SQL
	 */
	public SQLtableEditor(SQLadapter sql) {
		super(new BorderLayout());
		this.sql = sql;

		tableName = new JLabel();
		tableName.setHorizontalAlignment(SwingConstants.CENTER);
		add(tableName, BorderLayout.NORTH);

		add(new JScrollPane(table = new JTable(model = new AdvancedTableModel())), BorderLayout.CENTER);

		add(new CUDpanel(this), BorderLayout.SOUTH);
	}

	/**
	 * Função que estabelece qual tabela da base de dados será exibida
	 * 
	 * @param table nome da tabela SQL
	 */
	public void setTable(String table) {
		try {
			this.tableName.setText(table);
			ResultSet rs = sql.query("SELECT * FROM " + table);
			ResultSetMetaData rsmd = rs.getMetaData();

			// metadata da tabela
			int c = rsmd.getColumnCount();
			Object[] objs = new Object[c];
			this.tableTypes = new int[c];
			for (int i = 0; i < c; i++) {
				objs[i] = rsmd.getColumnName(i + 1);
				tableTypes[i] = rsmd.getColumnType(i + 1);
			}
			// cabeçalho das colunas
			model.setColumnIdentifiers(objs);

			// chaves primárias
			List<String> primary = sql.getPrimaryKeys(table);
			keys = new int[primary.size()];
			int i = 0;
			for (String p : primary)
				keys[i++] = ArrayUtils.indexOf(objs, p);

			// entradas
			model.clear();
			while (rs.next()) {
				for (i = 0; i < c; i++)
					objs[i] = rs.getObject(i + 1);
				model.addRow(objs);
			}

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();

		int selected = table.getSelectedRow();
		Object[] oldValues = null;

		switch (command) {
		case CUDpanel.EDIT:
			if (selected >= 0)
				oldValues = model.getRow(selected);
			else
				break;
		case CUDpanel.NEW:
			if (oldValues == null) {
				oldValues = SQLadapter.defaultObjects(tableTypes);
				selected = -1;
			}

			String[] fields = ArrayUtils.toStringA(model.getColumnIdentifiers());
			Object[] newValues = FillingFields.fillFields(SwingUtils.getWindow(this), "Inserir valores", fields, oldValues);

			if (newValues != null) {
				String table = tableName.getText();
				if (selected == -1) // novo
					sql.update("INSERT INTO `" + table + "`(`" + StringUtils.addSeparator(fields, "`, `") + "`) VALUES ("
							+ StringUtils.addSeparator(SQLadapter.prepareObjects(newValues, sql.getType(), tableTypes), ", ")
							+ ")");
				else // editar
					sql.update("UPDATE `" + table + "` SET "
							+ SQLadapter.getSet(fields, newValues, oldValues, sql.getType())
							+ getWhere(fields, oldValues, keys, sql.getType()));
			}
			break;
		case CUDpanel.DELETE:
			if (selected >= 0)
				oldValues = model.getRow(selected);
			else
				break;

			boolean erase = JOptionPane.showConfirmDialog(SwingUtils.getWindow(this), "Deseja eliminar o número?",
					"Apagar número", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

			if (erase) // apagar
				sql.update("DELETE FROM `" + tableName.getText() + "`"
						+ getWhere(ArrayUtils.toStringA(model.getColumnIdentifiers()), oldValues, keys, sql.getType()));
			break;
		}
	}

	private static String getWhere(String[] allFields, Object[] allValues, int[] primaryKeys, DatabaseEngine type) {
		String[] fields = null;
		Object[] values = null;
		if (primaryKeys.length > 0) {
			// se há chaves primárias
			fields = new String[primaryKeys.length];
			values = new Object[primaryKeys.length];
			for (int i = 0; i < primaryKeys.length; i++) {
				fields[i] = allFields[primaryKeys[i]];
				values[i] = allValues[primaryKeys[i]];
			}
		} else {
			fields = allFields;
			values = allValues;
		}
		return SQLadapter.getWhere(fields, values, type);
	}
}
