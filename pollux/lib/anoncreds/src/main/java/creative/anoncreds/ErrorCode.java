package creative.anoncreds;

import jnr.ffi.util.EnumMapper;

public enum ErrorCode implements EnumMapper.IntegerEnum {
    SUCCESS(0),
    INPUT(1),
    IOERROR(2),
    INVALIDSTATE(3),
    UNEXPECTED(4),
    CREDENTIALREVOKED(5),
    INVALIDUSERREVOCID(6),
    PROOFREJECTED(7),
    REVOCATIONREGISTRYFULL(8);

    private final int value;

    ErrorCode(final int value) {
        this.value = value;
    }

    @Override
    public int intValue() {
        return value;
    }
}
