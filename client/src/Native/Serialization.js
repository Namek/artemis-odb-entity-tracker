var _user$project$Native_Serialization = function() {

var int8 = new Int8Array(8);
var int32 = new Int32Array(int8.buffer, 0, 1);
var float32 = new Float32Array(int8.buffer, 0, 1);
var float64 = new Float64Array(int8.buffer, 0, 1);


function intBitsToFloat(i) {
	int32[0] = i;
	return float32[0];
}

function floatToIntBits(f) {
	float32[0] = f;
	return int32[0];
}

function intBitsToDouble(i1, i2) {
	int32[0] = i1;
	int32[1] = i2;
	return float64[0];
}

return {
  intBitsToFloat: intBitsToFloat,
  floatToIntBits: floatToIntBits,
  intBitsToDouble: intBitsToDouble
};

}();
/* SHIM Native.Serialization */