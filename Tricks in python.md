### Tricks in python

* bool is a subclass of int in python, but numpy.bool is clearly class irrelevant to int  
* when you compare bool with numpy.bool_ use ==
* EX: 

```python
import numpy as np

o = np.dot(np.array([1,3,8]), np.array([2,9,0]))

(o > 3) is True  # False

(o > 3) == True  # True

print(id(o > 3), id(True))
print(type(o > 3), type(True))
```
