## input,output,properties都是Map<String,Object>结构 

``````
input:[
        {
            "datatype":"aizoo.tensor",
            "name":"input",
            "description":"The input tensor."
        }
      ]
      
output:[
        {
            "datatype":"aizoo.tensor",
            "name":"output",
            "description":"The output tensor."
        }
       ]
       
properties:{
                "alpha":{
                            "defaultType":"number",
                            "type":"number",
                            "value":1,
                            "default":"alpha"
                         },
                "inplace":{
                            "defaultType":"bool",
                            "type":"bool",
                            "value":false,
                            "default":"inplace"
                            },
                "_description":"1.alpha : the $\\alpha$ value for the CELU formulation. 
                                Default: 1.0\n2.inplace :can optionally do the operation in-place. Default: False"}
``````