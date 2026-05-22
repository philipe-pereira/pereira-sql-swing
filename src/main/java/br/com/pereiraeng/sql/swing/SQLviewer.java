package br.com.pereiraeng.sql.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import br.com.pereiraeng.core.StringUtils;
import br.com.pereiraeng.core.collections.ArrayUtils;
import br.com.pereiraeng.icons.PereiraIcon;
import br.com.pereiraeng.office.sql.OfficeSql;
import br.com.pereiraeng.sql.SQLadapter;
import br.com.pereiraeng.sql.SQLconfig;
import br.com.pereiraeng.sql.DatabaseEngine;
import br.com.pereiraeng.sql.XMLsql;
import br.com.pereiraeng.swing.App;
import br.com.pereiraeng.swing.Grade;
import br.com.pereiraeng.swing.Janela;
import br.com.pereiraeng.swing.SwingUtils;
import br.com.pereiraeng.swing.dialog.FillingFields;
import br.com.pereiraeng.swing.input.file.FileChooser;
import br.com.pereiraeng.swing.input.file.FileFilterAdapter;
import br.com.pereiraeng.swing.list.filter.FilterableList;
import br.com.pereiraeng.swing.table.AdvancedTableModel;
import br.com.pereiraeng.swing.table.Table;

public class SQLviewer implements App, ActionListener, ListSelectionListener, ChangeListener {

	public static void main(String[] args) {
		Janela.startAlone(new SQLviewer());
	}

	// ---------------- base de dados ----------------

	private SQLadapter sql;

	private XMLsql xml;

	// ---------------- parte gráfica ----------------

	private JInternalFrame frame1;
	private JFrame frame2;

	private JComboBox<String> tablesCb;
	private JLabel labelDB, labelTable;

	private List<JLabel> labelList;
	private List<FilterableList<Object>> flList;
	private Grade lists;
	private JSpinner pos;

	private Table ev;

	public SQLviewer() {
		ev = new Table(0, 1);
		ev.setTableHeader(null);
		ev.setRowHeaderWidth(80);
		ev.setPreferredSize(new Dimension(203, 203));
	}

	@Override
	public void build(Component comp) {
		JPanel p = new JPanel(new BorderLayout());
		if (comp instanceof JInternalFrame) {
			this.frame1 = (JInternalFrame) comp;
			this.frame1.setContentPane(p);
		} else if (comp instanceof JFrame) {
			this.frame2 = (JFrame) comp;
			this.frame2.setContentPane(p);
		}

		// ====================================

		// barra principal

		p.add(SwingUtils.getBar(new String[][][] {
				{ { PereiraIcon.IMPORT.getPath(), "I", "Conexão a uma BD SQL cujos parâmetros estão num arquivo XML" },
						{ PereiraIcon.EDIT.getPath(), "E",
								"Conexão a uma BD SQL cujos parâmetros serão digitados pelo usuário" } },
				{ { PereiraIcon.OPEN.getPath(), "O", "Carregar arquivo XML para leitura de uma das tabelas da BD SQL" },
						{ PereiraIcon.CLOSE.getPath(), "U", "Descarregar arquivo XML para leitura da tabela" } },
				{ { PereiraIcon.EXPORT.getPath(), "e", "Exportar tabela" },
						{ PereiraIcon.TAB.getPath(), "f", "Listar campos da tbela" } } },
				this), BorderLayout.NORTH);

		Grade g = new Grade();

		labelDB = new JLabel();
		labelDB.setHorizontalAlignment(SwingConstants.CENTER);
		g.add(labelDB, 0, 0, 2, 1);

		tablesCb = new JComboBox<>();
		tablesCb.addActionListener(this);
		tablesCb.setActionCommand("T");
		g.add(tablesCb, 0, 1, 2, 1);

		labelTable = new JLabel();
		labelTable.setHorizontalAlignment(SwingConstants.CENTER);
		g.add(labelTable, 0, 2, 2, 1);

		this.labelList = new LinkedList<>();
		this.flList = new LinkedList<>();
		JScrollPane sp = new JScrollPane(this.lists = new Grade(), JScrollPane.VERTICAL_SCROLLBAR_NEVER,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setPreferredSize(new Dimension(100, 180));
		g.add(sp, 0, 3, 1, 2);

		this.pos = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		this.pos.addChangeListener(this);

		g.add(ev, 1, 3, 1, 2);

		p.add(g, BorderLayout.CENTER);
	}

	private String[] getFields() {
		String[] out = new String[labelList.size()];
		int i = 0;
		for (JLabel l : labelList)
			out[i++] = l.getText();
		return out;
	}

	private void set(Object[] objs) {
		for (int i = 0; i < objs.length; i++) {
			Object obj = null;
			if (objs[i] instanceof byte[])
				obj = "b[" + StringUtils.toHex((byte[]) objs[i]) + "]";
			else
				obj = objs[i];
			ev.setValueAt(obj, i, 0);
		}
	}

	private void loadDataBase(File xmlfile) {
		this.sql = new SQLadapter(xmlfile);
		loadDataBase();
	}

	private void loadDataBase(DatabaseEngine serverType, String server, String port, String login, String password, String db) {
		this.sql = new SQLadapter(serverType, server, port, login, password, db);
		loadDataBase();
	}

	private void loadDataBase() {
		this.sql.config();
		if (sql.connectDB()) {
			// nome da base de dados
			labelDB.setText(sql.getDB());

			// carregar todas as tabelas
			innerChange = true;
			tablesCb.removeAllItems();
			List<String> items = sql.getTables();
			for (String t : items)
				tablesCb.addItem(t);
			tablesCb.setSelectedIndex(-1);
			innerChange = false;
		} else
			JOptionPane.showMessageDialog(frame1, "Não foi possível se estabelecer à base de dados",
					"Conexão não estabelecida", JOptionPane.ERROR_MESSAGE);
	}

	private void loadTable(String table) {
		// nome da tabela
		labelTable.setText(table);

		// preparar o objeto EntryView para apresentar as entradas
		List<String> fields = sql.getFields(table);
		ev.setRowCount(fields.size());
		ev.setRowIdentifiers(fields.toArray(new String[fields.size()]));

		// procurar alguma chave primária
		List<String> keys = sql.getPrimaryKeys(table);

		this.lists.removeAll();
		this.labelList.clear();
		this.flList.clear();

		if (keys.size() != 0) {
			// se tem alguma chave, elas serão utilizadas

			int i = 0;
			for (String key : keys) {
				// nome da coluna da chave a ser considerada
				JLabel label = new JLabel(key, SwingConstants.CENTER);
				labelList.add(label);
				lists.add(label, i, 0, 1, 1);

				FilterableList<Object> fl = new FilterableList<>();
				fl.addListSelectionListener(this);
				fl.setPreferredSize(new Dimension(80, 160));
				flList.add(fl);
				lists.add(fl, i, 1, 1, 1);

				// carregar todos as entradas da chave
				Set<Object> objs = new LinkedHashSet<>();
				sql.list(objs, table, key); // TODO por LIMIT por conta de Java
											// heap space
				for (Object obj : objs)
					fl.addElement(obj);

				i++;
			}

			// as listas filtráveis não vem com nenhum elemento selecionado, de modo que a
			// tabela 'ev' deve ser resetada
			ev.remove();
		} else {
			// se não tem chave primária
			int entries = sql.getRowCount(table);
			SpinnerNumberModel snm = (SpinnerNumberModel) pos.getModel();
			snm.setValue(0);
			snm.setMaximum(entries - 1);
			// (a função 'setMaximum' dispara um 'stateChange' no JSpinner, de modo que a
			// tabela 'ev' já aparecerá com os valores relativos ao valor selecionado no
			// JSpinner)
			lists.add(pos, 0, 0, 1, 1);
		}

		lists.repaint();
		lists.revalidate();
	}

	private void loadEntry(Object... keys) {
		Object[] objs = null;
		if (keys != null) {
			if (xml == null) // modo geral: baixa todos os campos de uma tabela
				objs = sql.search(labelTable.getText(), getFields(), keys);
			else // modo xml: baixa só o que interessa
				objs = sql.search(xml.getQueryH(keys[0]));
		}
		set(objs);
	}

	// ---------------------- XML SQL ----------------------

	private void loadXML(File file) {
		xml = new XMLsql();
		xml.parse(file);

		String mainTable = xml.getMainTable();
		if (mainTable != null) {
			// preparar o objeto EntryView para apresentar as entradas
			String[] fields = xml.getFields();
			ev.setRowCount(fields.length);
			ev.setRowIdentifiers(fields);

			if (labelTable != null) {
				// se houver uma lista de chaves a ser exibida (as vezes quer-se somente uma
				// dada entrada, não sendo visível a lista de todas as chaves)

				// nome da tabela
				labelTable.setText(xml.getDbName());

				// escolher entre as tabelas
				tablesCb.setVisible(false);

				// ----------- lista de chaves -----------

				// nome da coluna da chave a ser considerada
				lists.removeAll();
				labelList.clear();
				flList.clear();

				// nome da coluna da chave a ser considerada
				JLabel label = new JLabel(xml.getPrimary(), SwingConstants.CENTER);
				labelList.add(label);
				lists.add(label, 0, 0, 1, 1);

				FilterableList<Object> fl = new FilterableList<>();
				fl.addListSelectionListener(this);
				fl.setPreferredSize(new Dimension(80, 160));
				flList.add(fl);
				lists.add(fl, 0, 1, 1, 1);

				// carregar todos as entradas da chave
				List<Object> objs = new LinkedList<>();
				sql.list(objs, xml.getQueryV());
				fl.clear();
				for (Object obj : objs)
					fl.addElement(obj);
			}
		}
	}

	private void unloadXML() {
		xml = null;

		// nome da tabela
		labelTable.setText("");

		lists.removeAll();

		// nomes das colunas da chave a ser considerada
		labelList.clear();

		// preparar o objeto EntryView para apresentar as entradas
		ev.clear();

		// carregar todas as entradas da chave
		flList.clear();

		tablesCb.setSelectedIndex(-1);
		tablesCb.setVisible(true);
	}

	// ------------------------- APP -------------------------

	@Override
	public String getTitle() {
		return "SQL viewer";
	}

	@Override
	public boolean isResizable() {
		return false;
	}

	@Override
	public boolean isMaximizable() {
		return false;
	}

	@Override
	public Dimension getWindowSize() {
		return new Dimension(320, 340);
	}

	@Override
	public void open(String file) {
		if (sql == null) // conexão a uma base de dados
			loadDataBase(new File(file));
		else // carregar uma estrutura de tabela
			loadXML(new File(file));
	}

	@Override
	public void start() {
	}

	@Override
	public void close() {
		if (sql != null)
			sql.disconnectDB();
	}

	// ------------------------- LISTENER -------------------------

	private transient boolean innerChange = false;

	@Override
	public void actionPerformed(ActionEvent event) {
		char ope = event.getActionCommand().charAt(0);

		switch (ope) {
		case 'I': // conectar a uma base de dados SQL cujos parâmetros estão num
					// arquivo XML
			File f = FileChooser.fileChooserLoad("files", new FileFilterAdapter("xml"), this.frame2);
			if (f != null)
				loadDataBase(f);
			break;
		case 'E': // entrada manual
			Object[] oldValues = null;
			if (sql == null)
				oldValues = SQLconfig.NULL;
			else {
				oldValues = sql.getConfig().getObjects();
				if ("".equals(oldValues[2]))
					oldValues[2] = -1;
				else
					oldValues[2] = Integer.parseInt((String) oldValues[2]);
			}
			Object[] params = FillingFields.fillFields(this.frame2, "Conectar à base de dados", SQLconfig.HEADER,
					oldValues);
			if (params != null) {
				int porta = (int) params[2];
				loadDataBase((DatabaseEngine) params[0], (String) params[1], porta < 0 ? "" : String.valueOf(porta),
						(String) params[3], (String) params[4], (String) params[5]);
			}
			break;
		case 'T': // abrir o conteúdo de uma dada tabela
			if (!innerChange) {
				String table = (String) tablesCb.getSelectedItem();
				if (table != null)
					loadTable(table);
			}
			break;
		case 'O': // leitura de uma tabela da base de dados segundo uma dada
					// regra, definida num arquivo XML
			File file = FileChooser.fileChooserLoad("files", new FileFilterAdapter("xml"), this.frame2);
			if (file != null)
				loadXML(file);
			break;
		case 'U':
			// descarregar o sistema de leitura da tabela, definida num arquivo
			// XML previamente carregado
			if (xml != null)
				unloadXML();
			break;
		case 'e': // exportar base de dados para tabela XLSX
			String table = labelTable.getText();
			if ("".equals(table))
				return;
			file = FileChooser.fileChooserSave(null, "xlsx", this.frame2);
			if (file != null) {
				if (xml == null)
					// modo geral: baixa todos os campos de uma tabela
					OfficeSql.export(file, sql.query("SELECT * FROM `" + table + "` WHERE 1"));
				else // modo xml: baixa só o que interessa
					OfficeSql.export(file, sql.query(xml.getAllQuery()));
			}
			break;
		case 'f': // listar campos da tabela
			table = labelTable.getText();
			if ("".equals(table))
				return;

			AdvancedTableModel atm = new AdvancedTableModel(
					new String[] { "Nome", "Tipo", "Tamanho", "Dígitos", "Nulo?", "PK?", "Sinal?", "Índice?" }, 0);
			atm.setColumnClass(Boolean.class, 4, 5, 6, 7);

			if (xml == null) { // modo geral: baixa todos os campos de uma
								// tabela
				Map<String, Object[]> fields = sql.getFieldsNData(table);
				for (Entry<String, Object[]> e : fields.entrySet()) {
					Object[] os = ArrayUtils.concatArray(new Object[] { e.getKey() }, e.getValue());
					os[1] = SQLadapter.getNames((int) os[1]);
					atm.addRow(os);
				}
			} else { // modo xml: baixa só o que interessa (indicado no XML)
				String[] fas = xml.getFields(false);
				Map<String, Set<String>> t2f = new LinkedHashMap<>();
				for (int i = 0; i < fas.length; i++) {
					String[] tf = fas[i].split("\\.");
					Set<String> fs = t2f.get(tf[0]);
					if (fs == null)
						t2f.put(tf[0], fs = new LinkedHashSet<>());
					fs.add(tf[1]);
				}

				for (Entry<String, Set<String>> e1 : t2f.entrySet()) {
					Map<String, Object[]> fields = sql.getFieldsNData(e1.getKey());
					for (String field : e1.getValue()) {
						Object[] os = ArrayUtils.concatArray(new Object[] { field }, fields.get(field));
						os[1] = SQLadapter.getNames((int) os[1]);
						atm.addRow(os);
					}
				}
			}

			JTable t = new JTable(atm);
			SwingUtils.setColumnsWidth(t, new int[] { 80, 40, 80, 50 });
			JScrollPane sp = new JScrollPane(t);
			sp.setPreferredSize(new Dimension(400, 200));

			JOptionPane.showMessageDialog(null, sp, "Campos", JOptionPane.INFORMATION_MESSAGE);
			break;
		}
	}

	@Override
	public void valueChanged(ListSelectionEvent event) {
		if (!event.getValueIsAdjusting()) {
			// se a tabela possui chaves primárias
			Object[] keys = new Object[this.flList.size()];
			int i = 0;
			for (FilterableList<?> keyList : this.flList) {
				Object o = keyList.get();
				if (o == null)
					return;
				keys[i++] = o;
			}
			loadEntry(keys);
		}
	}

	@Override
	public void stateChanged(ChangeEvent event) {
		Object[] objs = null;
		int pos = (int) this.pos.getValue();

		if (xml == null) // modo geral: baixa todos os campos de uma tabela
			objs = sql.search(labelTable.getText(), pos);
		else // modo xml: baixa só o que interessa
			objs = sql.search(xml.getQueryH(pos));

		set(objs);
	}

	// ====================================================================

	public static Table getTable(File xmlDB, File xmlTab, Object key) {
		SQLviewer sqlEd = new SQLviewer();
		sqlEd.loadDataBase(xmlDB);
		return getTable(sqlEd, xmlTab, key);
	}

	public static Table getTable(SQLadapter sql, File xmlTab, Object key) {
		SQLviewer sqlEd = new SQLviewer();
		sqlEd.sql = sql;
		return getTable(sqlEd, xmlTab, key);
	}

	private static Table getTable(SQLviewer sqlEd, File xmlTab, Object key) {
		sqlEd.loadXML(xmlTab);
		sqlEd.loadEntry(key);
		return sqlEd.ev;
	}
}
