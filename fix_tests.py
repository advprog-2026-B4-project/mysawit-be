import re

# Read the file
with open('src/test/java/id/ac/ui/cs/advprog/mysawitbe/modules/auth/application/service/AuthCommandUseCaseImplTest.java', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace Map.of with null email
pattern1 = r'Map<String, Object> tokens = Map\.of\(\s+"email",\s+null,\s+"name",\s+"User"\s+\);'
replacement1 = '''Map<String, Object> tokens = new HashMap<>();
        tokens.put("email", null);
        tokens.put("name", "User");'''
content = re.sub(pattern1, replacement1, content)

# Replace Map.of with null name  
pattern2 = r'Map<String, Object> tokens = Map\.of\(\s+"email",\s+"user@example\.com",\s+"name",\s+null\s+\);'
replacement2 = '''Map<String, Object> tokens = new HashMap<>();
        tokens.put("email", "user@example.com");
        tokens.put("name", null);'''
content = re.sub(pattern2, replacement2, content)

# Write the file back
with open('src/test/java/id/ac/ui/cs/advprog/mysawitbe/modules/auth/application/service/AuthCommandUseCaseImplTest.java', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed Map.of() instances with null values")
