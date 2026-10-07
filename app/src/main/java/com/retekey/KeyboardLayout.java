package com.retekey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A uniform orthogonal key grid.
 *
 * <p>Every row spans exactly {@link #columns()} columns and every key occupies a whole number of
 * columns, so keys are never staggered and only a declared span makes a key wider. Column and row
 * edges are computed from the view size, which keeps geometry and hit testing identical.
 */
public final class KeyboardLayout {
    private final KeyboardLayoutId id;
    private final boolean shifted;
    private final int columns;
    private final List<List<SoftwareKeySpec>> rows;
    /** Relative height of each row, or null when every row is the same height. */
    private final float[] rowWeights;
    /** Running total of {@link #rowWeights}: {@code rowStarts[i]} is where row i begins. */
    private final float[] rowStarts;
    /** Vertical gap, in dp, left above and below every key face. Drawing only. */
    private float verticalGapDp = DEFAULT_VERTICAL_GAP_DP;
    /** Grid columns that make up one ordinary key: 1 normally, 2 on the half-key Samsung grid. */
    private int columnsPerKey = 1;

    /** What every layout used before layouts could ask for their own. */
    public static final float DEFAULT_VERTICAL_GAP_DP = 2.0f;

    private KeyboardLayout(
        KeyboardLayoutId id,
        boolean shifted,
        int columns,
        List<List<SoftwareKeySpec>> rows,
        float[] rowWeights
    ) {
        if (id == null) {
            throw new IllegalArgumentException("layout id must not be null");
        }
        if (columns < 1) {
            throw new IllegalArgumentException("layout needs at least one column");
        }
        if (rows == null || rows.isEmpty()) {
            throw new IllegalArgumentException("layout needs at least one row");
        }
        for (List<SoftwareKeySpec> row : rows) {
            if (row.isEmpty()) {
                throw new IllegalArgumentException("layout rows must not be empty");
            }
            int spanned = 0;
            for (SoftwareKeySpec key : row) {
                spanned += key.columnSpan();
            }
            if (spanned != columns) {
                throw new IllegalArgumentException(
                    "every row must span exactly " + columns + " columns but one spans " + spanned
                );
            }
        }
        if (rowWeights != null) {
            if (rowWeights.length != rows.size()) {
                throw new IllegalArgumentException("one weight per row is required");
            }
            for (float weight : rowWeights) {
                if (!(weight > 0.0f)) {
                    throw new IllegalArgumentException("row weights must be positive");
                }
            }
        }
        this.id = id;
        this.shifted = shifted;
        this.columns = columns;
        this.rows = rows;
        this.rowWeights = rowWeights == null ? null : rowWeights.clone();
        this.rowStarts = new float[rows.size() + 1];
        for (int i = 0; i < rows.size(); i++) {
            rowStarts[i + 1] = rowStarts[i] + (rowWeights == null ? 1.0f : rowWeights[i]);
        }
    }

    public static KeyboardLayout of(
        KeyboardLayoutId id,
        boolean shifted,
        int columns,
        List<List<SoftwareKeySpec>> rows
    ) {
        List<List<SoftwareKeySpec>> copied = new ArrayList<>(rows.size());
        for (List<SoftwareKeySpec> row : rows) {
            copied.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        return new KeyboardLayout(id, shifted, columns, Collections.unmodifiableList(copied), null);
    }

    /** Like {@link #of}, but each row has its own height, in proportion to {@code rowWeights}. */
    public static KeyboardLayout of(
        KeyboardLayoutId id,
        boolean shifted,
        int columns,
        List<List<SoftwareKeySpec>> rows,
        float[] rowWeights
    ) {
        List<List<SoftwareKeySpec>> copied = new ArrayList<>(rows.size());
        for (List<SoftwareKeySpec> row : rows) {
            copied.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        return new KeyboardLayout(
            id, shifted, columns, Collections.unmodifiableList(copied), rowWeights);
    }

    /** A copy-free setter for builders: the layout is not shared until it is returned. */
    public KeyboardLayout withVerticalGapDp(float gapDp) {
        this.verticalGapDp = gapDp;
        return this;
    }

    /** Declares how many grid columns one ordinary key spans, so strips can be sized in keys. */
    public KeyboardLayout withColumnsPerKey(int columnsPerKey) {
        if (columnsPerKey < 1 || columns % columnsPerKey != 0) {
            throw new IllegalArgumentException("columns must be a whole number of keys");
        }
        this.columnsPerKey = columnsPerKey;
        return this;
    }

    public int columnsPerKey() {
        return columnsPerKey;
    }

    public float verticalGapDp() {
        return verticalGapDp;
    }

    public static List<SoftwareKeySpec> row(SoftwareKeySpec... keys) {
        return Collections.unmodifiableList(Arrays.asList(keys.clone()));
    }

    public KeyboardLayoutId id() {
        return id;
    }

    public boolean shifted() {
        return shifted;
    }

    public int columns() {
        return columns;
    }

    public List<List<SoftwareKeySpec>> rows() {
        return rows;
    }

    public SoftwareKeySpec findById(String stableKeyId) {
        if (stableKeyId == null) {
            return null;
        }
        for (List<SoftwareKeySpec> row : rows) {
            for (SoftwareKeySpec key : row) {
                if (stableKeyId.equals(key.stableKeyId())) {
                    return key;
                }
            }
        }
        return null;
    }

    /** First column occupied by the key at {@code keyIndex} of {@code rowIndex}. */
    public int startColumn(int rowIndex, int keyIndex) {
        List<SoftwareKeySpec> keys = rows.get(rowIndex);
        int column = 0;
        for (int i = 0; i < keyIndex; i++) {
            column += keys.get(i).columnSpan();
        }
        return column;
    }

    /** Pixel edge of a column boundary, shared by drawing and hit testing. */
    public int columnEdge(int column, int width) {
        return column * width / columns;
    }

    /** Pixel edge of a row boundary, shared by drawing and hit testing. */
    public int rowEdge(int rowIndex, int height) {
        if (rowWeights == null) {
            return rowIndex * height / rows.size();
        }
        if (rowIndex >= rows.size()) {
            return height;
        }
        return Math.round(height * rowStarts[rowIndex] / rowStarts[rows.size()]);
    }

    /** The row a vertical position falls in, honouring row weights. Callers check bounds. */
    public int rowAt(float y, int height) {
        if (rowWeights == null) {
            return Math.min(rows.size() - 1, (int) (y * rows.size() / height));
        }
        for (int i = 1; i < rows.size(); i++) {
            if (y < rowEdge(i, height)) {
                return i - 1;
            }
        }
        return rows.size() - 1;
    }

    public SoftwareKeySpec keyAt(float x, float y, int width, int height) {
        if (width <= 0 || height <= 0 || x < 0.0f || y < 0.0f || x >= width || y >= height) {
            return null;
        }
        int rowIndex = rowAt(y, height);
        int column = Math.min(columns - 1, (int) (x * columns / width));
        List<SoftwareKeySpec> keys = rows.get(rowIndex);
        int cursor = 0;
        for (SoftwareKeySpec key : keys) {
            cursor += key.columnSpan();
            if (column < cursor) {
                return key;
            }
        }
        return keys.get(keys.size() - 1);
    }
}
