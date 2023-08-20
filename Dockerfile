FROM devopsedu/webapp

# Set the working directory in the container
WORKDIR /var/www/html

# Copy your PHP application files into the container
COPY ./website/ /var/www/html/

# Expose port 80 for Apache
EXPOSE 80

# Start Apache in the foreground
# CMD ["apache2-foreground"]
CMD apachectl -D FOREGROUND
