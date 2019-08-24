#include<iostream>
#include<string>
 
using namespace std;
 
 
int main()
{
	string str1;
 
	while (cin >> str1)
	{
		int i = str1.length() ;//指向末尾的指针
		int j = i -1;//指向前一个
 
		while (j >= 0)
		{
			if (isupper(str1[j]))//如果是大写字母
			{
				char temp = str1[j];
				int t = j;
				i--;
				while (t < i)//将j-i间的小写字母前移
				{
					str1[t] = str1[t+1];
					t++;
				}
				str1[i] = temp;
			}
			j--;
		}
		cout << str1 << endl;
		
	}
	
	return 0;
}
