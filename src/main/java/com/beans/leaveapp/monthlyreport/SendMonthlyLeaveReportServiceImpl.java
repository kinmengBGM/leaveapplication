package com.beans.leaveapp.monthlyreport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.springframework.stereotype.Service;

import com.beans.exceptions.BSLException;
import com.beans.leaveapp.employee.model.Employee;
import com.beans.leaveapp.employee.repository.EmployeeRepository;
import com.beans.leaveapp.leavetransaction.repository.LeaveTransactionRepository;
import com.beans.leaveapp.leavetype.repository.LeaveTypeRepository;
import com.beans.leaveapp.yearlyentitlement.model.YearlyEntitlement;
import com.beans.leaveapp.yearlyentitlement.repository.YearlyEntitlementRepository;
import com.beans.util.email.EmailSender;
@Service
public class SendMonthlyLeaveReportServiceImpl implements SendMonthlyLeaveReportService{

	@Resource
	EmployeeRepository employeeRepository;
	
	@Resource
	YearlyEntitlementRepository entitlementRepository;
	
	@Resource
	LeaveTransactionRepository leaveRepository;
	
	@Override
	public void sendMonthlyLeaveReportToEmployees() {
		try{
		// Selecting all employee who are PERM and CONT which excludes roles with ROLE_ADMIN and ROLE_OPERDIR
		List<Employee> employeeList = employeeRepository.findAllEmployeesForSendingMonthlyLeaveReport();
		if(employeeList!=null && employeeList.size()>0){
			
			for (Employee employee : employeeList) {
				
				List<YearlyEntitlement>	employeeEntitlementList = entitlementRepository.findByEmployeeId(employee.getId());
				if(employeeEntitlementList!=null && employeeEntitlementList.size()>0){
					
					StringBuffer entilementData = new StringBuffer();
					for (YearlyEntitlement entitlement : employeeEntitlementList) {
						entilementData.append("<tr><td>"+entitlement.getLeaveType().getDescription()+"</td>");
						entilementData.append("<td>"+entitlement.getEntitlement()+"</td>");
						entilementData.append("<td>"+entitlement.getCurrentLeaveBalance()+"</td>");
						entilementData.append("<td>"+entitlement.getYearlyLeaveBalance()+"</td></tr>");
					}
					sendEmailMontlyLeaveReportToEmployee(employee, entilementData.toString());
				}
			}
		}
		}catch(Exception e){
			System.out.println("Error while sending mails to employees of monthly leave report");
			e.printStackTrace();
		}
	}
	private void sendEmailMontlyLeaveReportToEmployee(Employee employee,String entitlementData){
	
	try {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream("monthlyLeaveReportTemplate.html");
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

		// read file into string
		String line = null;

		StringBuilder responseData = new StringBuilder();
		try {
			while ((line = bufferedReader.readLine()) != null) {
				responseData.append(line);
			}
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		// load your HTML email template
		String htmlEmailTemplate = responseData.toString();
		
		// create the email message
		HtmlEmail email = new HtmlEmail();

		// replacing the employee details in HTML
		htmlEmailTemplate = htmlEmailTemplate.replace("##employeeName##",employee.getName());
		htmlEmailTemplate = htmlEmailTemplate.replace("##entitlementData##",entitlementData);
		// add reciepiant email address
		try {
			if(employee.getWorkEmailAddress()!=null && !employee.getWorkEmailAddress().equals(""))
			email.addTo(employee.getWorkEmailAddress(), employee.getName());
			email.setSubject("Reg : Monthly Leave Report of "+employee.getName());
		} catch (EmailException e) {
			e.printStackTrace();
		}
		// set the html message
		try {
			email.setHtmlMsg(htmlEmailTemplate);
		} catch (EmailException e2) {
			e2.printStackTrace();
		}

		// send the email
			EmailSender.sendEmail(email);
			System.out.println("Monthly Leave Report Email has been sent successfully to Employee "+employee.getName());
		
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Error while sending email to Employee "+employee.getName());
		}
	}
}
