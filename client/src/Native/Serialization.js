var _user$project$Native_Serialization = function() {

var int8 = new Int8Array(8);
var int32 = new Int32Array(int8.buffer, 0, 1);
var float32 = new Float32Array(int8.buffer, 0, 1);
var float64 = new Float64Array(int8.buffer, 0, 1);
var MOVE_32_BITS = Math.pow(2, 32);


function intBitsToFloat(i) {
	int32[0] = i;
	return float32[0];
}

function floatToIntBits(f) {
	float32[0] = f;
	return int32[0];
}

function intBitsToDouble(int1, int2) {
	int32[0] = int1;
	int32[1] = int2;
	return float64[0];
}

function intBitsToLong(int1, int2) {
	return int1 * MOVE_32_BITS + int2;
}

return {
  intBitsToFloat: intBitsToFloat
, floatToIntBits: floatToIntBits
, intBitsToDouble: F2(intBitsToDouble)
, intBitsToLong: F2(intBitsToLong)
};

}();
/* SHIM Native.Serialization */