# $Header$
# awk script to extract the opcode enumeration from PUC-Rio's lopcodes.h
# file and convert it to a Java fragment.
# Use like this:
# awk -f opcode.awk < lopcodes.h > tmp
BEGIN{x=0}
/^typedef enum/,/^}/ {
  if(match($0, /OP_[A-Z]*/)) {
    print "private static final int " substr($0, RSTART, RLENGTH) " = " x++ ";"
  }
}
