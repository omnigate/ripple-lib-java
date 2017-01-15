package com.ripple.core.fields;

import com.ripple.core.formats.Format;
import com.ripple.core.formats.LEFormat;
import com.ripple.core.formats.TxFormat;
import com.ripple.core.serialized.enums.LedgerEntryType;
import com.ripple.core.serialized.enums.TransactionType;
import com.ripple.utils.TestHelpers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.EnumMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class FieldSymbolicsTest {
    @Test
    public void CheckFields() throws FileNotFoundException {
        FileReader reader = TestHelpers.getResourceReader("protocol.json");
        JSONObject o = new JSONObject(new JSONTokener(reader));
        JSONArray fields = o.getJSONArray("fields");
        JSONArray transactions = o.getJSONArray("transactions");
        JSONArray entries = o.getJSONArray("ledgerEntries");

        checkFields(fields);
        checkTransactions(transactions);
        checkEntries(entries);
    }

    private void checkTransactions(JSONArray txns) {
        assertEquals(txns.length(),
                TransactionType.values().length);

        for (int i = 0; i < txns.length(); i++) {
            JSONObject tx = txns.getJSONObject(i);
            String txName = tx.getString("name");
            if (!txName.isEmpty()) {
                try {
                    TransactionType.valueOf(txName);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("missing TransactionType " +
                              txName);
                }
                Format txFormat = TxFormat.fromString(txName);
                assertNotNull(txFormat);
                checkFormat(tx, txFormat);
            }
        }
    }

    private void checkFormat(JSONObject obj, Format format) {
        String txName = obj.getString("name");

        if (format == null) {
            throw new IllegalArgumentException();
        }
        EnumMap<Field, Format.Requirement> requirements = format.requirements();
        JSONArray fields = obj.getJSONArray("fields");

        for (int j = 0; j < fields.length(); j++) {
            JSONArray field = fields.getJSONArray(j);
            String fieldName = field.getString(0);
            String requirement = field.getString(1);

            Field key = Field.fromString(fieldName);
            if (!requirements.containsKey(key)) {
                throw new RuntimeException(
                        String.format("%s format missing %s %s %n",
                        txName, requirement, fieldName));
            } else {
                Format.Requirement req = requirements.get(key);
                if (!req.toString().equals(requirement)) {
                    throw new RuntimeException(
                            String.format("%s format missing %s %s %n",
                                txName, requirement, fieldName));

                }
            }
        }
        // check length is same, and if none are missing, must be equal ;)
        assertEquals(obj.toString(2),
                fields.length(), requirements.size());
    }

    private void checkEntries(JSONArray entries) {
        assertEquals(entries.length(), LedgerEntryType.values().length);
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entryJson = entries.getJSONObject(i);
            String name = entryJson.getString("name");
            if (!name.isEmpty()) {
                try {
                    LedgerEntryType.valueOf(name);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("missing LedgerEntryType for " +
                            entryJson);
                }
                LEFormat format = LEFormat.fromString(name);
                assertNotNull(format);
                checkFormat(entryJson, format);
            }
        }
    }

    private void checkFields(JSONArray fields) {
        TreeSet<String> names = new TreeSet<String>();
        for (int i = 0; i < fields.length(); i++) {
            JSONObject fieldJson = fields.getJSONObject(i);
            String nam = fieldJson.getString("name");
            names.add(nam);
            if (!nam.isEmpty()) {
                try {
                    Field f = Field.valueOf(fieldJson.getString("name"));
                    Type t = Type.valueOf(fieldJson.getString("type"));
                    assertEquals(fieldJson.toString(2), f.type.id, t.id);
                    assertEquals(fieldJson.toString(2), f.id, fieldJson
                            .getInt("value"));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Can't find Field or Type for "
                            + fieldJson);
                }
            }
        }
        for (Field field : Field.values()) {
            if (field.isSerialized() && !names.contains(field.name())) {
                if (!((field == Field.ArrayEndMarker) ||
                        (field == Field.ObjectEndMarker)))
                    throw new AssertionError(field.toString());
            }
        }
    }
}
