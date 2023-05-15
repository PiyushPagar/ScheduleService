
package com.revnomix.revseed.Service;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.revnomix.revseed.model.EventLog;
import com.revnomix.revseed.repository.EventLogRepository;
import com.revnomix.revseed.Util.StringUtil;

@Service
public class EventLogService {

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private EntityManager entityManager;
//
//    @Autowired
//    private UserRestController userRestController;
//
//    public List<EventLog> search(EventLogFilter eventLogFilter) {
//        List<EventLog> list = new ArrayList<>();
//
//        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
//        CriteriaQuery<EventLog> query = cb.createQuery(EventLog.class);
//
//        Root<EventLog> root = query.from(EventLog.class);
//
//        List<Predicate> predicates = new ArrayList<>();
//
//        if (null != eventLogFilter.getAction() && StringUtil.isNotEmpty(eventLogFilter.getAction())) {
//            Path<String> exp = root.get("action");
//            predicates.add(cb.equal(exp, eventLogFilter.getAction()));
//        }
//        if (null != eventLogFilter.getIpAddress() && StringUtil.isNotEmpty(eventLogFilter.getIpAddress())) {
//            Path<String> exp = root.get("ipAddress");
//            predicates.add(cb.equal(exp, eventLogFilter.getIpAddress()));
//        }
//        if (null != eventLogFilter.getMessage() && StringUtil.isNotEmpty(eventLogFilter.getMessage())) {
//            Path<String> exp = root.get("message");
//            predicates.add(cb.equal(exp, eventLogFilter.getMessage()));
//        }
//        if (null != eventLogFilter.getSource() && StringUtil.isNotEmpty(eventLogFilter.getSource())) {
//            Path<String> exp = root.get("source");
//            predicates.add(cb.equal(exp, eventLogFilter.getSource()));
//        }
//        if (null != eventLogFilter.getAccountId()) {
//            Path<String> exp = root.get("accountId");
//            predicates.add(cb.equal(exp, eventLogFilter.getAccountId()));
//        }
//        if (null != eventLogFilter.getClientId()) {
//            Path<String> exp = root.get("clientId");
//            predicates.add(cb.equal(exp, eventLogFilter.getClientId()));
//        }
//        if (null != eventLogFilter.getFromDate() && StringUtil.isNotEmpty(eventLogFilter.getFromDate())) {
//            Path<Date> exp = root.get("createdDate");
//            predicates.add(cb.greaterThanOrEqualTo(exp, DateUtil.toDateByAllFormat(eventLogFilter.getFromDate(), "yyyy-MM-dd")));
//        }
//        if (null != eventLogFilter.getToDate() && StringUtil.isNotEmpty(eventLogFilter.getToDate())) {
//            Path<Date> exp = root.get("createdDate");
//            predicates.add(cb.lessThanOrEqualTo(exp, DateUtil.toDateByAllFormat(eventLogFilter.getToDate(), "yyyy-MM-dd")));
//        }
//
//        query.select(root).where(cb.and(predicates.toArray(new Predicate[predicates.size()])));
//        entityManager.createQuery(query).getResultList().forEach(log->{
//            list.add(log);
//        });
//        return list;
//    }

    public EventLog save(EventLog eventLog) {
        return eventLogRepository.save(eventLog);
    }
    public EventLog saveLog(String source, String action, Object response, String message, HttpServletRequest request, Integer clientId) {
//        JwtUser user = userRestController.getAuthenticatedUser(request);
        EventLog eventLog = new EventLog();
        eventLog.setAction(action);
        eventLog.setSource(source);
        eventLog.setMessage(message);
        eventLog.setClientId(clientId);
        String ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ip == null){
            ip = request.getRemoteAddr();
        }
        eventLog.setIpAddress(ip);
//        if (user != null){
//            eventLog.setAccountId(user.getId());
//            eventLog.setUsername(user.getUsername());
//        }
        if(response!=null) {
	        String stringResponse = StringUtil.convertObjectToString(response);
	        eventLog.setResponse(stringResponse);
        }
        return eventLogRepository.save(eventLog);
    }
}

