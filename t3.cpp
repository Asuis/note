#include<iostream>
#include<algorithm>
#include<vector>
using namespace std;

int getMax(vector<int> &a,int n) {
    int max = a[0];
    int min = a[n-1];
    int c = 0;
    int p = 0;
    int i = 0;
    while(max==a[i]&&min==a[n-i-1]){
        c++;
        i++;
    }
    return c;
}
int getMin(vector<int> &a,int n) {
    int min = a[n-1];
    int c = 0;
    for(int i=1;i<n;i++) {
        if (a[i]-a[i-1]<min) {
            min = a[i] - a[i+1];
        }
    }
    if (min >0) {
        for (int i=1; i<n; i++) {
           if (a[i]-a[i-1] == min) {
              c++; 
            } 
        } 
    }else{
        for (int i=1; i<n; i++){ 
            int j=i-1;
                while (a[j]==a[i] && j>=0){
                    c++; 
                    j--; 
                }
            }
        } 
    return c;
}
int main() {
    int n;
    while(cin>>n){
        vector<int> a(n);
        for(int i=0;i<n;i++){
            cin>>a[i];
        }
        sort(a.begin(),a.end());
        cout<<getMax(a,n)<<" "<<getMin(a,n)<<endl;
    }
    
    return 0;
}