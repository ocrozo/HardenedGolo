module spec.Div

function div = |a, b| spec/
                        requires {
                          ((b + 5) < 0) \/ (b \/ 0)
                        }
                     /spec {
  return (a / b)
}

function test = {
	var myDiv = div(1, 2)
	myDiv = div(40, 1)
	return(myDiv)
}

function main = |args| {
	test()
}
