-- temperature conversion table (celsius to farenheit)

for c0=-20,50-1,10 do
	print("C ")
	for c=c0,c0+10-1 do
		print(string.format("%3.0f ",c))
	end
	print("\n")
	
	print("F ")
	for c=c0,c0+10-1 do
		f=(9/5)*c+32
		print(string.format("%3.0f ",f))
	end
	print("\n\n")
end
