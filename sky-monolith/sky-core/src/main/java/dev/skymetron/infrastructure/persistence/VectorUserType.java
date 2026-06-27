package dev.skymetron.infrastructure.persistence;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

/**
 * Hibernate 6 {@link UserType} that binds {@code float[]} to a PostgreSQL
 * {@code vector} column via {@link PGvector} (which extends {@code PGobject}).
 *
 * <p>The PostgreSQL JDBC driver recognises {@code PGobject} in
 * {@code PreparedStatement.setObject} and sends the value with the correct
 * {@code vector} type name.
 */
public class VectorUserType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position,
                               SharedSessionContractImplementor session, Object owner) throws SQLException {
        String value = rs.getString(position);
        if (value == null || rs.wasNull()) {
            return null;
        }
        return new PGvector(value).toArray();
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, new PGvector(value));
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return cached == null ? null : ((float[]) cached).clone();
    }
}
