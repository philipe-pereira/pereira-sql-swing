package br.com.pereiraeng.sql.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;

import br.com.pereiraeng.core.Password;
import br.com.pereiraeng.icons.Icons;
import br.com.pereiraeng.sql.SQLconfig;
import br.com.pereiraeng.sql.Server;
import br.com.pereiraeng.swing.SwingUtils;
import br.com.pereiraeng.swing.dialog.FillingFields;
import br.com.pereiraeng.swing.image.LabelPointer;
import br.com.pereiraeng.swing.image.Pointer;
import br.com.pereiraeng.swing.input.Input;
import br.com.pereiraeng.swing.input.file.FileChooser;
import br.com.pereiraeng.swing.input.file.FileFilterAdapter;

public class SQLconfigPanel extends JPanel implements Input<SQLconfig>, ActionListener {
	private static final long serialVersionUID = -5515304418783868550L;

	public static final String REFRESH = "REFRESH";

	private SQLconfig config;

	private LabelPointer l;

	public SQLconfigPanel() {

		JButton b = new JButton(Icons.loadUtilsIcon("Import24.gif"));
		b.setPreferredSize(SwingUtils.DIM_BUTTON_ICON);
		b.setToolTipText("Conexão a uma BD SQL cujos parâmetros estão num arquivo XML");
		b.addActionListener(this);
		b.setActionCommand("I");
		add(b);

		b = new JButton(Icons.loadUtilsIcon("Edit.gif"));
		b.setPreferredSize(SwingUtils.DIM_BUTTON_ICON);
		b.addActionListener(this);
		b.setToolTipText("Conexão a uma BD SQL cujos parâmetros serão digitados pelo usuário");
		b.setActionCommand("E");
		add(b);

		b = new JButton(Icons.loadUtilsIcon("Refresh.gif"));
		b.setToolTipText("Tentar (re)estabelecer conexão com a BD SQL");
		b.setPreferredSize(SwingUtils.DIM_BUTTON_ICON);
		b.addActionListener(this);
		b.setActionCommand(REFRESH);
		add(b);

		add(this.l = new LabelPointer(Pointer.NEUTRAL));
	}
	// métodos de interfaceamento

	/**
	 * 
	 * @param status ver {@link Pointer#setStatus(int)}
	 */
	public void setStatus(int status) {
		this.l.setStatus(status);
	}

	// listener's

	private transient ActionListener listener;

	public void setActionListener(ActionListener listener) {
		this.listener = listener;
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		switch (command) {
		case "I": // conectar a uma base de dados SQL cujos parâmetros estão num
			// arquivo XML
			File xmlfile = FileChooser.fileChooserLoad("files", new FileFilterAdapter("xml"), SwingUtils.getWindow(this));
			if (xmlfile != null)
				this.config = SQLconfig.loadConfig(xmlfile);
			break;
		case "E": // entrada manual
			Object[] oldValues = null;
			if (config == null)
				oldValues = SQLconfig.NULL;
			else {
				oldValues = config.getObjects();
				if ("".equals(oldValues[2]))
					oldValues[2] = -1;
				else
					oldValues[2] = Integer.parseInt((String) oldValues[2]);

				oldValues[4] = new Password((String) oldValues[4]);
			}
			Object[] params = FillingFields.fillFields(SwingUtils.getWindow(this), "Conectar à base de dados",
					SQLconfig.HEADER, oldValues);
			if (params != null) {
				int porta = (int) params[2];
				this.config = new SQLconfig((Server) params[0], (String) params[1],
						porta < 0 ? "" : String.valueOf(porta), (String) params[3], (String) params[4],
						(String) params[5]);
				// TODO usar SQLconfig#askLogin
			}
			break;
		default:
			if (this.listener != null)
				this.listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command));
			break;

		}
	}

	@Override
	public void set(SQLconfig config) {
		this.config = config;
	}

	@Override
	public SQLconfig get() {
		return this.config;
	}

	@Override
	public Component getComponent() {
		return this;
	}
}
