function receivePushNotification(event) {
  console.log("push received");

  
  const message = event.data.text();
  const title = message.name + " in " + message.topic;

  const options = {
    //data: url,
    body: notificationText
    icon: "https://entermediadb.org/entermediadb/mediadb/services/module/asset/downloads/preset/2019/12/f0/94a/image200x200.png"
  };
  //call the method showNotification to show the notification
  event.waitUntil(self.registration.showNotification(title, options));
}
self.addEventListener("push", receivePushNotification);