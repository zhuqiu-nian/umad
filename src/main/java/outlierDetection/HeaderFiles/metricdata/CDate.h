//CDate.h
#pragma once

class CDate
{
public:
    CDate(int year = 1900, int month = 1, int day = 1);
    ~CDate() {}
    bool IsValid();
    int GetMonthDay(int year, int month);
    bool IsLeapYear(int year);
    void Show();

    CDate& operator=(const CDate& d);
    bool operator==(const CDate& d);
    bool operator>=(const CDate& d);
    bool operator<=(const CDate& d);
    bool operator!=(const CDate& d);
    bool operator>(const CDate& d);
    bool operator<(const CDate& d);
    CDate operator+(int day);
    CDate& operator+=(int day);
    CDate operator-(int day);
    CDate& operator-=(int day);
    int operator-(const CDate& d);//日期-日期 返回天数
    CDate& operator++();//默认前置
    CDate operator++(int);//用参数标志后置++
    CDate& operator--();
    CDate operator--(int);

private:
    int _year;
    int _month;
    int _day;
};