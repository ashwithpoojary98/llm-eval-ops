#!/usr/bin/env python
"""
Script to create/recreate evaluation engine database tables.
Run this after updating the database models.
"""
import asyncio
from sqlalchemy import text

from app.database import engine, Base
from app.models import db_models  # noqa - import to register models


async def main():
    """Create all tables defined in the models."""

    # Check existing tables
    print("Checking existing tables...")
    async with engine.connect() as conn:
        result = await conn.execute(text("""
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            ORDER BY table_name
        """))
        tables = result.fetchall()
        print(f"\nExisting tables in database:")
        for (table_name,) in tables:
            print(f"  - {table_name}")

    # Create tables
    print("\nCreating evaluation engine tables...")
    async with engine.begin() as conn:
        # Uncomment the next line to drop and recreate tables
        # await conn.run_sync(Base.metadata.drop_all)

        await conn.run_sync(Base.metadata.create_all)

    print("\nTables created/verified:")
    for table in Base.metadata.tables:
        print(f"  - {table}")

    # Dispose engine to close all connections properly
    await engine.dispose()
    print("\nDone!")


if __name__ == "__main__":
    asyncio.run(main())
