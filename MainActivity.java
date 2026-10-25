    private void showCRM() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = vertical();
        root.setBackgroundColor(pale);
        pad(root, 14);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setBackground(shape(navy, 22));
        pad(header, 18);

        header.addView(text(
            "VIPIN SERVICE CRM", 23, Color.WHITE, true
        ));
        header.addView(text(
            "Offline Service & Collection Manager",
            13, Color.LTGRAY, false
        ));
        root.addView(header, lp(-1, -2));
        addGap(root, 12);

        totalsBox = vertical();
        totalsBox.setBackground(shape(Color.WHITE, 18));
        pad(totalsBox, 14);
        root.addView(totalsBox, lp(-1, -2));

        root.addView(text(
            "ADD NEW RECORD", 18, navy, true
        ));
        addGap(root, 8);

        dateText = text(
            "Date: " + selectedDate + " (Tap to change)",
            15, blue, true
        );
        dateText.setBackground(shape(Color.WHITE, 14));
        pad(dateText, 15);
        root.addView(dateText, lp(-1, 50));

        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });

        addGap(root, 7);

        billInput = edit("Bill Number", 2);
        customerInput = edit("Customer Name", 1);
        mobileInput = edit("Mobile Number", 3);
        productInput = edit("Product Name", 1);
        amcInput = edit("AMC Amount (₹)", 8194);
        collectionInput = edit("Collection Amount (₹)", 8194);
        notesInput = edit("Remarks (optional)", 1);

        addField("BILL NUMBER", billInput);
        addField("CUSTOMER NAME", customerInput);
        addField("MOBILE NUMBER", mobileInput);
        addField("PRODUCT NAME", productInput);
        addField("AMC AMOUNT", amcInput);
        addField("COLLECTION AMOUNT", collectionInput);
        addField("REMARKS", notesInput);

        Button save = button("＋ SAVE RECORD", green);
        root.addView(save, lp(-1, 52));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRecord();
            }
        });

        addGap(root, 10);

        Button export = button("EXPORT TO EXCEL (CSV)", blue);
        root.addView(export, lp(-1, 50));
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportCsv();
            }
        });

        addGap(root, 18);
        root.addView(text(
            "MONTH / YEAR RECORDS", 18, navy, true
        ));
        addGap(root, 6);

        LinearLayout filterRow = horizontal();
        final Spinner monthSpinner = new Spinner(this);
        final Spinner yearSpinner = new Spinner(this);

        String[] months = {
            "All Months", "January", "February",
            "March", "April", "May", "June",
            "July", "August", "September",
            "October", "November", "December"
        };

        ArrayAdapter<String> ma = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, months
        );
        ma.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        monthSpinner.setAdapter(ma);

        String[] years = yearChoices();
        ArrayAdapter<String> ya = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, years
        );
        ya.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        yearSpinner.setAdapter(ya);

        filterRow.addView(
            monthSpinner, new LinearLayout.LayoutParams(0, 48, 1)
        );
        filterRow.addView(
            yearSpinner, new LinearLayout.LayoutParams(0, 48, 1)
        );
        root.addView(filterRow);

        recordsBox = vertical();
        root.addView(recordsBox, lp(-1, -2));

        AdapterView.OnItemSelectedListener filterListener =
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                AdapterView<?> parent, View view,
                int position, long id
            ) {
                updateRecords(
                    monthSpinner.getSelectedItemPosition(),
                    yearSpinner.getSelectedItem().toString()
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        monthSpinner.setOnItemSelectedListener(filterListener);
        yearSpinner.setOnItemSelectedListener(filterListener);

        Button changePass = button("CHANGE PASSWORD", purple);
        root.addView(changePass, lp(-1, 48));
        changePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        updateTotals();
        updateRecords(0, "All Years");
        setContentView(scroll);
    }

    private void addField(String label, EditText field) {
        TextView t = text(label, 11, navy, true);
        root.addView(t);
        root.addView(field, lp(-1, 48));
        addGap(root, 5);
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();

        try {
            String[] p = selectedDate.split("-");
            c.set(
                Integer.parseInt(p[2]),
                Integer.parseInt(p[1]) - 1,
                Integer.parseInt(p[0])
            );
        } catch (Exception ignored) {}

        new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(
                    DatePicker view, int year,
                    int month, int day
                ) {
                    selectedDate = String.format(
                        Locale.getDefault(),
                        "%02d-%02d-%04d",
                        day, month + 1, year
                    );
                    dateText.setText(
                        "Date: " + selectedDate +
                        " (Tap to change)"
                    );
                }
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private double number(EditText e) {
        try {
            return Double.parseDouble(
                e.getText().toString().trim().replace(",", "")
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    private void saveRecord() {
        if (billInput.getText().toString().trim().isEmpty()) {
            billInput.setError("Enter bill number");
            return;
        }

        if (productInput.getText().toString().trim().isEmpty()) {
            productInput.setError("Enter product name");
            return;
        }

        try {
            JSONObject r = new JSONObject();
            r.put("date", selectedDate);
            r.put("bill", billInput.getText().toString().trim());
            r.put("customer", customerInput.getText().toString().trim());
            r.put("mobile", mobileInput.getText().toString().trim());
            r.put("product", productInput.getText().toString().trim());
            r.put("amc", number(amcInput));
            r.put("collection", number(collectionInput));
            r.put("remarks", notesInput.getText().toString().trim());

            records.put(r);
            prefs.edit().putString(RECORDS, records.toString()).apply();

            Toast.makeText(
                this, "Record saved offline",
                Toast.LENGTH_SHORT
            ).show();

            billInput.setText("");
            customerInput.setText("");
            mobileInput.setText("");
            productInput.setText("");
            amcInput.setText("");
            collectionInput.setText("");
            notesInput.setText("");

            updateTotals();
            updateRecords(0, "All Years");

        } catch (Exception e) {
            Toast.makeText(
                this, "Could not save record",
                Toast.LENGTH_LONG
            ).show();
        }
        }
    private void showCRM() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root = vertical();
        root.setBackgroundColor(pale);
        pad(root, 14);
        scroll.addView(root);

        LinearLayout header = vertical();
        header.setBackground(shape(navy, 22));
        pad(header, 18);

        header.addView(text(
            "VIPIN SERVICE CRM", 23, Color.WHITE, true
        ));
        header.addView(text(
            "Offline Service & Collection Manager",
            13, Color.LTGRAY, false
        ));
        root.addView(header, lp(-1, -2));
        addGap(root, 12);

        totalsBox = vertical();
        totalsBox.setBackground(shape(Color.WHITE, 18));
        pad(totalsBox, 14);
        root.addView(totalsBox, lp(-1, -2));

        root.addView(text(
            "ADD NEW RECORD", 18, navy, true
        ));
        addGap(root, 8);

        dateText = text(
            "Date: " + selectedDate + " (Tap to change)",
            15, blue, true
        );
        dateText.setBackground(shape(Color.WHITE, 14));
        pad(dateText, 15);
        root.addView(dateText, lp(-1, 50));

        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickDate();
            }
        });

        addGap(root, 7);

        billInput = edit("Bill Number", 2);
        customerInput = edit("Customer Name", 1);
        mobileInput = edit("Mobile Number", 3);
        productInput = edit("Product Name", 1);
        amcInput = edit("AMC Amount (₹)", 8194);
        collectionInput = edit("Collection Amount (₹)", 8194);
        notesInput = edit("Remarks (optional)", 1);

        addField("BILL NUMBER", billInput);
        addField("CUSTOMER NAME", customerInput);
        addField("MOBILE NUMBER", mobileInput);
        addField("PRODUCT NAME", productInput);
        addField("AMC AMOUNT", amcInput);
        addField("COLLECTION AMOUNT", collectionInput);
        addField("REMARKS", notesInput);

        Button save = button("＋ SAVE RECORD", green);
        root.addView(save, lp(-1, 52));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveRecord();
            }
        });

        addGap(root, 10);

        Button export = button("EXPORT TO EXCEL (CSV)", blue);
        root.addView(export, lp(-1, 50));
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportCsv();
            }
        });

        addGap(root, 18);
        root.addView(text(
            "MONTH / YEAR RECORDS", 18, navy, true
        ));
        addGap(root, 6);

        LinearLayout filterRow = horizontal();
        final Spinner monthSpinner = new Spinner(this);
        final Spinner yearSpinner = new Spinner(this);

        String[] months = {
            "All Months", "January", "February",
            "March", "April", "May", "June",
            "July", "August", "September",
            "October", "November", "December"
        };

        ArrayAdapter<String> ma = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, months
        );
        ma.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        monthSpinner.setAdapter(ma);

        String[] years = yearChoices();
        ArrayAdapter<String> ya = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, years
        );
        ya.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        );
        yearSpinner.setAdapter(ya);

        filterRow.addView(
            monthSpinner, new LinearLayout.LayoutParams(0, 48, 1)
        );
        filterRow.addView(
            yearSpinner, new LinearLayout.LayoutParams(0, 48, 1)
        );
        root.addView(filterRow);

        recordsBox = vertical();
        root.addView(recordsBox, lp(-1, -2));

        AdapterView.OnItemSelectedListener filterListener =
            new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                AdapterView<?> parent, View view,
                int position, long id
            ) {
                updateRecords(
                    monthSpinner.getSelectedItemPosition(),
                    yearSpinner.getSelectedItem().toString()
                );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        monthSpinner.setOnItemSelectedListener(filterListener);
        yearSpinner.setOnItemSelectedListener(filterListener);

        Button changePass = button("CHANGE PASSWORD", purple);
        root.addView(changePass, lp(-1, 48));
        changePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

        updateTotals();
        updateRecords(0, "All Years");
        setContentView(scroll);
    }

    private void addField(String label, EditText field) {
        TextView t = text(label, 11, navy, true);
        root.addView(t);
        root.addView(field, lp(-1, 48));
        addGap(root, 5);
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();

        try {
            String[] p = selectedDate.split("-");
            c.set(
                Integer.parseInt(p[2]),
                Integer.parseInt(p[1]) - 1,
                Integer.parseInt(p[0])
            );
        } catch (Exception ignored) {}

        new DatePickerDialog(
            this,
            new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(
                    DatePicker view, int year,
                    int month, int day
                ) {
                    selectedDate = String.format(
                        Locale.getDefault(),
                        "%02d-%02d-%04d",
                        day, month + 1, year
                    );
                    dateText.setText(
                        "Date: " + selectedDate +
                        " (Tap to change)"
                    );
                }
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private double number(EditText e) {
        try {
            return Double.parseDouble(
                e.getText().toString().trim().replace(",", "")
            );
        } catch (Exception ex) {
            return 0;
        }
    }

    private void saveRecord() {
        if (billInput.getText().toString().trim().isEmpty()) {
            billInput.setError("Enter bill number");
            return;
        }

        if (productInput.getText().toString().trim().isEmpty()) {
            productInput.setError("Enter product name");
            return;
        }

        try {
            JSONObject r = new JSONObject();
            r.put("date", selectedDate);
            r.put("bill", billInput.getText().toString().trim());
            r.put("customer", customerInput.getText().toString().trim());
            r.put("mobile", mobileInput.getText().toString().trim());
            r.put("product", productInput.getText().toString().trim());
            r.put("amc", number(amcInput));
            r.put("collection", number(collectionInput));
            r.put("remarks", notesInput.getText().toString().trim());

            records.put(r);
            prefs.edit().putString(RECORDS, records.toString()).apply();

            Toast.makeText(
                this, "Record saved offline",
                Toast.LENGTH_SHORT
            ).show();

            billInput.setText("");
            customerInput.setText("");
            mobileInput.setText("");
            productInput.setText("");
            amcInput.setText("");
            collectionInput.setText("");
            notesInput.setText("");

            updateTotals();
            updateRecords(0, "All Years");

        } catch (Exception e) {
            Toast.makeText(
                this, "Could not save record",
                Toast.LENGTH_LONG
            ).show();
        }
    }
    private String[] yearChoices() {
        java.util.ArrayList<String> list =
            new java.util.ArrayList<String>();

        list.add("All Years");
        int current = Calendar.getInstance().get(Calendar.YEAR);

        for (int y = current + 1; y >= current - 10; y--) {
            list.add(String.valueOf(y));
        }

        return list.toArray(new String[list.size()]);
    }

    private void updateTotals() {
        if (totalsBox == null) return;

        totalsBox.removeAllViews();
        double a = 0, c = 0;

        for (int i = 0; i < records.length(); i++) {
            JSONObject r = records.optJSONObject(i);
            if (r != null) {
                a += r.optDouble("amc", 0);
                c += r.optDouble("collection", 0);
            }
        }

        totalsBox.addView(text(
            "ALL-TIME SUMMARY", 14, navy, true
        ));

        LinearLayout row = horizontal();

        TextView amc = text(
            "AMC\n₹ " + money(a), 17, purple, true
        );
        TextView col = text(
            "COLLECTION\n₹ " + money(c), 17, green, true
        );

        row.addView(
            amc, new LinearLayout.LayoutParams(0, 58, 1)
        );
        row.addView(
            col, new LinearLayout.LayoutParams(0, 58, 1)
        );

        totalsBox.addView(row);
        totalsBox.addView(text(
            "Total records: " + records.length(),
            12, Color.DKGRAY, false
        ));
    }

    private String money(double n) {
        return String.format(Locale.getDefault(), "%.2f", n);
    }

    private void updateRecords(int monthPosition, String year) {
        if (recordsBox == null) return;

        recordsBox.removeAllViews();
        int shown = 0;

        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject r = records.optJSONObject(i);
            if (r == null) continue;

            String date = r.optString("date", "");
            String[] parts = date.split("-");
            if (parts.length != 3) continue;

            int m = 0;
            try {
                m = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}

            if (monthPosition > 0 && m != monthPosition) {
                continue;
            }

            if (!"All Years".equals(year) &&
                !parts[2].equals(year)) {
                continue;
            }

            LinearLayout card = vertical();
            card.setBackground(shape(Color.WHITE, 16));
            pad(card, 12);

            card.addView(text(
                r.optString("product") +
                " | Bill: " + r.optString("bill"),
                15, navy, true
            ));

            card.addView(text(
                date + "   " + r.optString("customer"),
                12, Color.DKGRAY, false
            ));

            card.addView(text(
                "AMC: ₹ " + money(r.optDouble("amc", 0)) +
                "   Collection: ₹ " +
                money(r.optDouble("collection", 0)),
                13, green, true
            ));

            if (!r.optString("mobile").isEmpty()) {
                card.addView(text(
                    "Mobile: " + r.optString("mobile"),
                    12, Color.DKGRAY, false
                ));
            }

            if (!r.optString("remarks").isEmpty()) {
                card.addView(text(
                    "Remarks: " + r.optString("remarks"),
                    12, Color.DKGRAY, false
                ));
            }

            recordsBox.addView(card, lp(-1, -2));
            addGap(recordsBox, 7);
            shown++;
        }

        if (shown == 0) {
            recordsBox.addView(text(
                "No records for this selection.",
                14, Color.GRAY, false
            ));
        }
    }

    private void exportCsv() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(
            Intent.EXTRA_TITLE,
            "VIPIN_SERVICE_CRM_Records.csv"
        );

        try {
            startActivityForResult(intent, exportRequest);
        } catch (Exception e) {
            Toast.makeText(
                this, "File export is not available",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == exportRequest &&
            resultCode == RESULT_OK && data != null) {

            Uri uri = data.getData();

            try {
                OutputStream out =
                    getContentResolver().openOutputStream(uri);

                OutputStreamWriter writer =
                    new OutputStreamWriter(out, "UTF-8");

                writer.write(
                    "Date,Bill Number,Customer,Mobile,Product," +
                    "AMC Amount,Collection Amount,Remarks\n"
                );

                for (int i = 0; i < records.length(); i++) {
                    JSONObject r = records.optJSONObject(i);
                    if (r == null) continue;

                    writer.write(
                        csv(r.optString("date")) + "," +
                        csv(r.optString("bill")) + "," +
                        csv(r.optString("customer")) + "," +
                        csv(r.optString("mobile")) + "," +
                        csv(r.optString("product")) + "," +
                        r.optDouble("amc", 0) + "," +
                        r.optDouble("collection", 0) + "," +
                        csv(r.optString("remarks")) + "\n"
                    );
                }

                writer.flush();
                writer.close();

                Toast.makeText(
                    this,
                    "Excel-compatible CSV exported",
                    Toast.LENGTH_LONG
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "Export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private String csv(String value) {
        return "\"" +
            value.replace("\"", "\"\"") +
            "\"";
    }

    private void changePassword() {
        final EditText pass = edit("New password", 129);

        new AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(pass)
            .setPositiveButton(
                "Save",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        DialogInterface dialog, int which
                    ) {
                        String p = pass.getText().toString();

                        if (p.trim().length() >= 4) {
                            prefs.edit()
                                .putString("app_password", p)
                                .apply();

                            Toast.makeText(
                                MainActivity.this,
                                "Password changed",
                                Toast.LENGTH_SHORT
                            ).show();

                        } else {
                            Toast.makeText(
                                MainActivity.this,
                                "Use at least 4 characters",
                                Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
            )
            .setNegativeButton("Cancel", null)
            .show();
    }
}
