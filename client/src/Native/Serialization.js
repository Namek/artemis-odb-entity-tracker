var _user$project$Native_Serialization = function() {

var int8 = new Int8Array(4);
var int32 = new Int32Array(int8.buffer, 0, 1);
var float32 = new Float32Array(int8.buffer, 0, 1);


function intBitsToFloat(i) {
	int32[0] = i;
	return float32[0];
}

function floatToIntBits(f) {
	float32[0] = f;
	return int32[0];
}

return {
  intBitsToFloat: intBitsToFloat,
  floatToIntBits: floatToIntBits
};

}();
/* SHIM Native.Serialization */