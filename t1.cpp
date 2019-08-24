#include<cstdio>
#include<algorithm>
#include<string.h>
#include<iostream>
using namespace std;

const int MAXN = 1010;
int temp[MAXN][MAXN];

int main()
{
    string str;
    string tmp;
    while(cin>>str){
        tmp = string(str);
        reverse(tmp.begin(), tmp.end());
        int len = str.length();
        memset(temp, 0,sizeof(temp));
        for(int i=0;i<len;i++){
            for(int j=0;j<len;j++){
                if(str[i]==tmp[j]) {
                    temp[i+1][j+1] = temp[i][j] + 1;
                } else {
                    temp[i+1][j+1] = max(temp[i][j+1],temp[i+1][j]);
                }
            }
        }
        cout<<len - temp[len][len]<<endl;
    }
    return 0;
}