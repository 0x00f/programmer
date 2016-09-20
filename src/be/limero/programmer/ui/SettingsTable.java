package be.limero.programmer.ui;

import java.beans.PropertyChangeSupport;
import java.util.Date;
import java.util.HashMap;

import javax.swing.table.AbstractTableModel;

import lombok.Data;

public class SettingsTable extends AbstractTableModel {
	@Data
	class Row {
		public Row(String _key, String _value, Date _date) {
			key = _key;
			value = _value;
			date = _date;
		}

		String key;
		String value;
		Date date;
	}

	HashMap<String, Row> map;

	public SettingsTable() {
		map = new HashMap<String, Row>();
	}

	@Override
	public int getRowCount() {

		return map.size();
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		String key = (String) map.keySet().toArray()[rowIndex];

		if (columnIndex == 0) {
			return key;
		} else if (columnIndex == 1) {
			return map.get(key).getValue();
		} else if (columnIndex == 2) {
			return map.get(key).getDate();
		}
		return "outside table";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		if (column == 0)
			return "Setting";
		else if (column == 1)
			return "Value";
		else if (column == 2)
			return "Time";
		return "unknown column";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object,
	 * int, int)
	 */
	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		super.setValueAt(aValue, rowIndex, columnIndex);
	}

	public void put(String key, String value) {
		map.put(key, new Row(key, value, new Date()));
		fireTableDataChanged();
	}
	
	public String get(String key) {
		return map.get(key).getValue();
	}

}
